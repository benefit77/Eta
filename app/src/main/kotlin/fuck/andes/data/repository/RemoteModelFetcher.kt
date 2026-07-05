package fuck.andes.data.repository

import fuck.andes.agent.model.AgentHttpClient
import fuck.andes.agent.model.CustomHeaderFilter
import fuck.andes.agent.model.ProviderUrls
import fuck.andes.data.model.AnthropicProviderSetting
import fuck.andes.data.model.Model
import fuck.andes.data.model.ProviderSetting
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

internal object RemoteModelFetcher {
    private const val MAX_ERROR_CHARS = 600
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(provider: ProviderSetting): Result<List<Model>> =
        withContext(Dispatchers.IO) {
            runCatching {
                when (provider) {
                    is AnthropicProviderSetting -> fetchAnthropic(provider)
                    else -> fetchOpenAiCompatible(provider)
                }
            }
        }

    internal fun parseOpenAiModels(body: String): List<Model> {
        val data = json.parseToJsonElement(body).jsonObject["data"]?.jsonArray ?: return emptyList()
        return data.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                val modelId = obj["id"]?.jsonPrimitive?.content?.trim().orEmpty()
                if (modelId.isBlank()) return@mapNotNull null
                val ownedBy = obj["owned_by"]?.jsonPrimitive?.content.orEmpty()
                modelFromId(modelId = modelId, displayName = modelId, ownedBy = ownedBy)
            }.getOrNull()
        }
    }

    internal fun parseAnthropicModels(body: String): List<Model> {
        val data = json.parseToJsonElement(body).jsonObject["data"]?.jsonArray ?: return emptyList()
        return data.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                val modelId = obj["id"]?.jsonPrimitive?.content?.trim().orEmpty()
                if (modelId.isBlank()) return@mapNotNull null
                val displayName = obj["display_name"]?.jsonPrimitive?.content?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: modelId
                modelFromId(modelId = modelId, displayName = displayName, ownedBy = "anthropic")
            }.getOrNull()
        }
    }

    private fun fetchOpenAiCompatible(provider: ProviderSetting): List<Model> {
        val request = Request.Builder()
            .url(ProviderUrls.openAiModelsUrl(provider.baseUrl))
            .headers(
                okhttp3.Headers.Builder()
                    .add("Accept", "application/json")
                    .apply {
                        if (provider.apiKey.isNotBlank()) {
                            add("Authorization", "Bearer ${provider.apiKey}")
                        }
                        CustomHeaderFilter.mergeInto(this, provider.customHeaders)
                    }
                    .build()
            )
            .get()
            .build()
        return executeJson(request, "拉取模型失败").let(::parseOpenAiModels)
    }

    private fun fetchAnthropic(provider: AnthropicProviderSetting): List<Model> {
        val request = Request.Builder()
            .url(ProviderUrls.anthropicModelsUrl(provider.baseUrl))
            .headers(
                okhttp3.Headers.Builder()
                    .add("Accept", "application/json")
                    .add("anthropic-version", provider.anthropicVersion)
                    .apply {
                        if (provider.apiKey.isNotBlank()) {
                            add("x-api-key", provider.apiKey)
                        }
                        CustomHeaderFilter.mergeInto(this, provider.customHeaders)
                    }
                    .build()
            )
            .get()
            .build()
        return executeJson(request, "拉取 Anthropic 模型失败").let(::parseAnthropicModels)
    }

    private fun executeJson(request: Request, errorPrefix: String): String =
        AgentHttpClient.client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                error("$errorPrefix HTTP ${response.code}: ${body.compactError()}")
            }
            body
        }

    private fun modelFromId(modelId: String, displayName: String, ownedBy: String): Model =
        Model(
            id = UUID.randomUUID().toString(),
            modelId = modelId,
            displayName = displayName,
            supportsVision = ModelCapabilityRegistry.supportsVision(modelId, ownedBy),
            supportsTools = ModelCapabilityRegistry.supportsTools(modelId, ownedBy),
            supportsReasoning = ModelCapabilityRegistry.supportsReasoning(modelId, ownedBy),
            contextWindow = ModelCapabilityRegistry.contextWindow(modelId, ownedBy)
        )

    private fun String.compactError(): String =
        replace('\n', ' ')
            .replace('\r', ' ')
            .let { if (it.length > MAX_ERROR_CHARS) it.take(MAX_ERROR_CHARS) + "..." else it }
}
