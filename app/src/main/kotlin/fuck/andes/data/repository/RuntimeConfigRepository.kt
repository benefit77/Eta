package fuck.andes.data.repository

import android.content.SharedPreferences
import fuck.andes.agent.model.AgentModelClient
import fuck.andes.config.Prefs
import fuck.andes.data.datastore.SettingsDataStore
import fuck.andes.data.model.AnthropicProviderSetting
import fuck.andes.data.model.CustomBody
import fuck.andes.data.model.CustomProviderSetting
import fuck.andes.data.model.Model
import fuck.andes.data.model.OpenAiCompatibleProviderSetting
import fuck.andes.data.model.ProviderSetting
import fuck.andes.data.model.ProviderTypes
import fuck.andes.data.model.Settings
import fuck.andes.data.model.runtimeProviderType
import fuck.andes.data.model.selectedOrFirstModel
import fuck.andes.data.model.withModels
import fuck.andes.data.provider.BuiltinProviders
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal object RuntimeConfigRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun selectedProviderIdFlow() = SettingsDataStore.selectedProviderIdFlow()

    fun selectedModelIdFlow() = SettingsDataStore.selectedModelIdFlow()

    suspend fun selectedProvider(): ProviderSetting? {
        val settings = SettingsDataStore.settings()
        return settings.providers.firstOrNull { it.id == settings.selectedProviderId }
    }

    suspend fun setSelectedProviderId(id: String?) {
        SettingsDataStore.updateSettings { settings ->
            val provider = settings.providers.firstOrNull { it.id == id && it.isEnabled }
            val model = provider?.selectedOrFirstModel(null)
            settings.copy(
                selectedProviderId = provider?.id,
                selectedModelId = model?.id
            ).repair()
        }
    }

    suspend fun setSelectedModelId(id: String?) {
        SettingsDataStore.updateSettings { settings ->
            val provider = settings.providers.firstOrNull { provider ->
                provider.models.any { it.id == id && it.isEnabled }
            } ?: return@updateSettings settings.repair()
            settings.copy(
                selectedProviderId = provider.id,
                selectedModelId = id
            ).repair()
        }
    }

    suspend fun currentRuntimeConfig(): AgentModelClient.ModelConfig? {
        val settings = SettingsDataStore.settings().repair()
        val provider = settings.providers.firstOrNull { it.id == settings.selectedProviderId } ?: return null
        val model = provider.selectedOrFirstModel(settings.selectedModelId) ?: return null
        return buildRuntimeConfig(provider, model)
    }

    suspend fun syncToRemotePreferences(service: XposedService?): Boolean {
        val prefs = Prefs.remotePreferencesForUi(service) ?: return false
        val config = currentRuntimeConfig() ?: return clearRuntimeConfig(prefs)
        return writeRuntimeConfig(prefs, config)
    }

    suspend fun migrateLegacyConfig(service: XposedService?) {
        SettingsDataStore.updateSettings { settings ->
            val withDefaults = if (settings.providers.isEmpty()) {
                defaultSettings(legacyMigrationCompleted = false)
            } else {
                settings
            }
            if (service == null || withDefaults.legacyMigrationCompleted) {
                withDefaults.repair()
            } else {
                migrateFromRemotePrefs(withDefaults, service).repair()
            }
        }
        syncToRemotePreferences(service)
    }

    suspend fun ensureDefaults(service: XposedService?) {
        migrateLegacyConfig(service)
    }

    fun runtimeConfigJson(config: AgentModelClient.ModelConfig): String =
        json.encodeToString(config)

    fun buildRuntimeConfig(provider: ProviderSetting, model: Model): AgentModelClient.ModelConfig {
        val systemPrompt = provider.systemPrompt
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: BuiltinProviders.DEFAULT_SYSTEM_PROMPT
        return AgentModelClient.ModelConfig(
            providerId = provider.id,
            providerName = provider.name,
            providerType = provider.runtimeProviderType,
            baseUrl = provider.baseUrl.trim(),
            apiKey = provider.apiKey.trim(),
            model = model.modelId.trim(),
            modelDisplayName = model.displayName.trim(),
            systemPrompt = systemPrompt,
            anthropicVersion = (provider as? AnthropicProviderSetting)?.anthropicVersion
                ?: AnthropicProviderSetting.DEFAULT_ANTHROPIC_VERSION,
            openAiEndpointMode = when (provider) {
                is OpenAiCompatibleProviderSetting -> provider.endpointMode
                is CustomProviderSetting -> provider.endpointMode
                is AnthropicProviderSetting -> ""
            },
            customHeaders = provider.customHeaders + model.customHeaders,
            customBody = provider.customBody + model.customBody
        )
    }

    private fun migrateFromRemotePrefs(settings: Settings, service: XposedService): Settings {
        val prefs = Prefs.remotePreferencesForUi(service) ?: return settings
        val legacyBaseUrl = prefs.getString(Prefs.Keys.AGENT_BASE_URL, "")?.trim().orEmpty()
        val legacyModel = prefs.getString(Prefs.Keys.AGENT_MODEL, "")?.trim().orEmpty()
        val legacyApiKey = prefs.getString(Prefs.Keys.AGENT_API_KEY, "")?.trim().orEmpty()
        val legacySystemPrompt = prefs.getString(
            Prefs.Keys.AGENT_SYSTEM_PROMPT,
            BuiltinProviders.DEFAULT_SYSTEM_PROMPT
        )?.trim().orEmpty()
        val legacyExtraBody = prefs.getString(Prefs.Keys.AGENT_EXTRA_BODY_JSON, "")?.trim().orEmpty()

        if (legacyBaseUrl.isBlank() || legacyModel.isBlank() || !settings.isOnlyBuiltInDefaults()) {
            return settings.copy(legacyMigrationCompleted = true)
        }

        val provider = CustomProviderSetting(
            id = ProviderRepository.newId(),
            name = inferProviderName(legacyBaseUrl),
            baseUrl = legacyBaseUrl,
            apiKey = legacyApiKey,
            systemPrompt = legacySystemPrompt.takeIf { it.isNotBlank() },
            sortOrder = 0,
            customBody = legacyExtraBody.toCustomBodyList(),
            models = listOf(
                Model(
                    id = ModelRepository.newId(),
                    modelId = legacyModel,
                    displayName = legacyModel,
                    supportsTools = true
                )
            )
        )
        val shiftedBuiltIns = settings.providers.map { it.copyShiftedOrder(1) }
        return settings.copy(
            providers = listOf(provider) + shiftedBuiltIns,
            selectedProviderId = provider.id,
            selectedModelId = provider.models.firstOrNull()?.id,
            legacyMigrationCompleted = true
        )
    }

    private fun Settings.isOnlyBuiltInDefaults(): Boolean =
        providers.isNotEmpty() && providers.all { it.isBuiltIn }

    private fun String.toCustomBodyList(): List<CustomBody> {
        if (isBlank()) return emptyList()
        val root = runCatching { json.parseToJsonElement(this).jsonObject }.getOrNull() ?: return emptyList()
        return root.entries.map { (key, value) -> CustomBody(key = key, value = value) }
    }

    private fun ProviderSetting.copyShiftedOrder(offset: Int): ProviderSetting =
        when (this) {
            is OpenAiCompatibleProviderSetting -> copy(sortOrder = sortOrder + offset)
            is AnthropicProviderSetting -> copy(sortOrder = sortOrder + offset)
            is CustomProviderSetting -> copy(sortOrder = sortOrder + offset)
        }

    private fun Settings.repair(): Settings =
        ProviderRepository.repair(this)

    private fun defaultSettings(legacyMigrationCompleted: Boolean): Settings {
        val provider = BuiltinProviders.PROVIDERS.first()
        val model = provider.models.minByOrNull { it.sortOrder }
        return Settings(
            providers = BuiltinProviders.PROVIDERS,
            selectedProviderId = provider.id,
            selectedModelId = model?.id,
            legacyMigrationCompleted = legacyMigrationCompleted
        )
    }

    private fun writeRuntimeConfig(
        prefs: SharedPreferences,
        config: AgentModelClient.ModelConfig
    ): Boolean =
        runCatching {
            prefs.edit()
                .putString(Prefs.Keys.AGENT_RUNTIME_CONFIG_JSON, runtimeConfigJson(config))
                .commit()
        }.getOrDefault(false)

    private fun clearRuntimeConfig(prefs: SharedPreferences): Boolean =
        runCatching {
            prefs.edit()
                .remove(Prefs.Keys.AGENT_RUNTIME_CONFIG_JSON)
                .commit()
        }.getOrDefault(false)

    private fun inferProviderName(baseUrl: String): String = when {
        baseUrl.contains("openai", ignoreCase = true) -> "OpenAI-compatible"
        baseUrl.contains("dashscope", ignoreCase = true) -> "阿里百炼"
        baseUrl.contains("deepseek", ignoreCase = true) -> "DeepSeek"
        baseUrl.contains("siliconflow", ignoreCase = true) -> "硅基流动"
        baseUrl.contains("openrouter", ignoreCase = true) -> "OpenRouter"
        baseUrl.contains("anthropic", ignoreCase = true) -> "Anthropic"
        else -> "迁移的自定义 Provider"
    }
}
