package fuck.andes.data.repository

import fuck.andes.data.datastore.SettingsDataStore
import fuck.andes.data.model.AnthropicProviderSetting
import fuck.andes.data.model.CustomProviderSetting
import fuck.andes.data.model.Model
import fuck.andes.data.model.OpenAiCompatibleProviderSetting
import fuck.andes.data.model.OpenAiEndpointMode
import fuck.andes.data.model.ProviderSetting
import fuck.andes.data.model.Settings
import fuck.andes.data.model.withApiKey
import fuck.andes.data.model.withModels
import fuck.andes.data.model.withSortOrder
import fuck.andes.data.provider.BuiltinProviders
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal object ProviderRepository {
    fun providersFlow(): Flow<List<ProviderSetting>> =
        SettingsDataStore.settingsFlow().map { it.providers.sortedBy(ProviderSetting::sortOrder) }

    fun settingsFlow(): Flow<Settings> =
        SettingsDataStore.settingsFlow()

    suspend fun settings(): Settings =
        SettingsDataStore.settings()

    suspend fun allProviders(): List<ProviderSetting> =
        SettingsDataStore.settings().providers.sortedBy(ProviderSetting::sortOrder)

    suspend fun providerById(id: String): ProviderSetting? =
        SettingsDataStore.settings().providers.firstOrNull { it.id == id }

    suspend fun addProvider(provider: ProviderSetting): ProviderSetting {
        var added = provider
        SettingsDataStore.updateSettings { settings ->
            val nextOrder = (settings.providers.maxOfOrNull { it.sortOrder } ?: -1) + 1
            added = provider.withSortOrder(nextOrder)
            settings.copy(providers = settings.providers + added)
        }
        return added
    }

    suspend fun addCustomOpenAiProvider(): ProviderSetting =
        addProvider(
            CustomProviderSetting(
                id = newId(),
                name = "自定义 OpenAI-compatible",
                baseUrl = "https://api.example.com/v1",
                endpointMode = OpenAiEndpointMode.CHAT_COMPLETIONS,
                models = listOf(
                    Model(
                        id = newId(),
                        modelId = "model-id",
                        displayName = "自定义模型",
                        supportsTools = true
                    )
                )
            )
        )

    suspend fun addAnthropicProvider(): ProviderSetting =
        addProvider(
            AnthropicProviderSetting(
                id = newId(),
                name = "自定义 Anthropic",
                baseUrl = "https://api.anthropic.com",
                models = listOf(
                    Model(
                        id = newId(),
                        modelId = "claude-sonnet-5",
                        displayName = "Claude Sonnet 5",
                        supportsVision = true,
                        supportsTools = true,
                        supportsReasoning = true,
                        contextWindow = 1_000_000
                    )
                )
            )
        )

    suspend fun updateProvider(provider: ProviderSetting) {
        SettingsDataStore.updateSettings { settings ->
            settings.copy(
                providers = settings.providers.map { current ->
                    if (current.id == provider.id) provider else current
                }
            ).repairSelection()
        }
    }

    suspend fun deleteProvider(id: String) {
        SettingsDataStore.updateSettings { settings ->
            val provider = settings.providers.firstOrNull { it.id == id } ?: return@updateSettings settings
            if (provider.isBuiltIn) return@updateSettings settings
            settings.copy(providers = settings.providers.filterNot { it.id == id }).repairSelection()
        }
    }

    suspend fun copyProvider(id: String): ProviderSetting? {
        var copy: ProviderSetting? = null
        SettingsDataStore.updateSettings { settings ->
            val source = settings.providers.firstOrNull { it.id == id } ?: return@updateSettings settings
            val nextOrder = (settings.providers.maxOfOrNull { it.sortOrder } ?: -1) + 1
            copy = source.deepCopy(
                id = newId(),
                name = "${source.name} 副本",
                sortOrder = nextOrder,
                builtIn = false
            )
            settings.copy(providers = settings.providers + checkNotNull(copy))
        }
        return copy
    }

    suspend fun resetBuiltIn(id: String) {
        val builtIn = BuiltinProviders.providerById(id) ?: return
        SettingsDataStore.updateSettings { settings ->
            val current = settings.providers.firstOrNull { it.id == id }
            settings.copy(
                providers = settings.providers.map { provider ->
                    if (provider.id == id) {
                        if (current == null) builtIn else builtIn
                            .withApiKey(current.apiKey)
                            .withSortOrder(current.sortOrder)
                    } else {
                        provider
                    }
                }
            ).repairSelection()
        }
    }

    suspend fun ensureBuiltInsMerged() {
        SettingsDataStore.updateSettings { settings ->
            if (settings.providers.isEmpty()) {
                BuiltinProviders.defaultSettings()
            } else {
                val existingIds = settings.providers.mapTo(mutableSetOf()) { it.id }
                val missing = BuiltinProviders.PROVIDERS.filterNot { it.id in existingIds }
                if (missing.isEmpty()) settings else settings.copy(
                    providers = (settings.providers + missing).sortedBy(ProviderSetting::sortOrder)
                ).repairSelection()
            }
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun repair(settings: Settings): Settings =
        settings.repairSelection()

    private fun ProviderSetting.deepCopy(
        id: String,
        name: String,
        sortOrder: Int,
        builtIn: Boolean
    ): ProviderSetting {
        val copiedModels = models.mapIndexed { index, model ->
            model.copy(id = newId(), isBuiltIn = builtIn, sortOrder = index)
        }
        return when (this) {
            is OpenAiCompatibleProviderSetting -> copy(
                id = id,
                name = name,
                sortOrder = sortOrder,
                isBuiltIn = builtIn,
                models = copiedModels
            )
            is AnthropicProviderSetting -> copy(
                id = id,
                name = name,
                sortOrder = sortOrder,
                isBuiltIn = builtIn,
                models = copiedModels
            )
            is CustomProviderSetting -> copy(
                id = id,
                name = name,
                sortOrder = sortOrder,
                isBuiltIn = builtIn,
                models = copiedModels
            )
        }
    }

    private fun Settings.repairSelection(): Settings {
        val sortedProviders = providers.sortedBy(ProviderSetting::sortOrder)
        val selectedProvider = sortedProviders.firstOrNull { it.id == selectedProviderId && it.isEnabled }
            ?: sortedProviders.firstOrNull { it.isEnabled }
        val selectedModel = selectedProvider?.models
            ?.firstOrNull { it.id == selectedModelId && it.isEnabled }
            ?: selectedProvider?.models
                ?.filter { it.isEnabled }
                ?.minByOrNull { it.sortOrder }
        return copy(
            providers = sortedProviders,
            selectedProviderId = selectedProvider?.id,
            selectedModelId = selectedModel?.id
        )
    }

    private fun BuiltinProviders.defaultSettings(): Settings {
        val provider = PROVIDERS.first()
        val model = provider.models.minByOrNull { it.sortOrder }
        return Settings(
            providers = PROVIDERS,
            selectedProviderId = provider.id,
            selectedModelId = model?.id
        )
    }
}
