package fuck.andes.data.provider

import fuck.andes.data.model.AnthropicProviderSetting
import fuck.andes.data.model.Model
import fuck.andes.data.model.OpenAiCompatibleProviderSetting
import fuck.andes.data.model.ProviderSetting

internal object BuiltinProviders {
    const val DEFAULT_SYSTEM_PROMPT =
        "你是运行在 Android 设备上的手机 Agent。回答要简洁、直接，并保留必要的操作上下文。"

    const val OPENAI_ID = "builtin-openai"
    const val ANTHROPIC_ID = "builtin-anthropic"
    const val DASHSCOPE_ID = "builtin-dashscope"
    const val DEEPSEEK_ID = "builtin-deepseek"
    const val SILICONFLOW_ID = "builtin-siliconflow"
    const val OPENROUTER_ID = "builtin-openrouter"

    val PROVIDERS: List<ProviderSetting> = listOf(
        OpenAiCompatibleProviderSetting(
            id = OPENAI_ID,
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            isBuiltIn = true,
            sortOrder = 0,
            systemPrompt = DEFAULT_SYSTEM_PROMPT,
            models = listOf(
                Model(
                    id = "builtin-openai-gpt-5-5",
                    modelId = "gpt-5.5",
                    displayName = "GPT-5.5",
                    isBuiltIn = true,
                    supportsVision = true,
                    supportsTools = true,
                    supportsReasoning = true
                )
            )
        ),
        AnthropicProviderSetting(
            id = ANTHROPIC_ID,
            name = "Anthropic",
            baseUrl = "https://api.anthropic.com",
            isBuiltIn = true,
            sortOrder = 1,
            systemPrompt = DEFAULT_SYSTEM_PROMPT,
            models = listOf(
                Model(
                    id = "builtin-anthropic-claude-fable-5",
                    modelId = "claude-fable-5",
                    displayName = "Claude Fable 5",
                    isBuiltIn = true,
                    supportsVision = true,
                    supportsTools = true,
                    supportsReasoning = true,
                    contextWindow = 1_000_000
                ),
                Model(
                    id = "builtin-anthropic-claude-opus-4-8",
                    modelId = "claude-opus-4-8",
                    displayName = "Claude Opus 4.8",
                    isBuiltIn = true,
                    sortOrder = 1,
                    supportsVision = true,
                    supportsTools = true,
                    supportsReasoning = true,
                    contextWindow = 1_000_000
                ),
                Model(
                    id = "builtin-anthropic-claude-sonnet-5",
                    modelId = "claude-sonnet-5",
                    displayName = "Claude Sonnet 5",
                    isBuiltIn = true,
                    sortOrder = 2,
                    supportsVision = true,
                    supportsTools = true,
                    supportsReasoning = true,
                    contextWindow = 1_000_000
                )
            )
        ),
        OpenAiCompatibleProviderSetting(
            id = DASHSCOPE_ID,
            name = "阿里百炼",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            isBuiltIn = true,
            sortOrder = 2,
            systemPrompt = DEFAULT_SYSTEM_PROMPT,
            models = listOf(
                Model(
                    id = "builtin-dashscope-qwen3-7-max",
                    modelId = "qwen3.7-max",
                    displayName = "Qwen3.7 Max",
                    isBuiltIn = true,
                    supportsVision = true,
                    supportsTools = true,
                    supportsReasoning = true,
                    contextWindow = 1_000_000
                ),
                Model(
                    id = "builtin-dashscope-qwen3-7-plus",
                    modelId = "qwen3.7-plus",
                    displayName = "Qwen3.7 Plus",
                    isBuiltIn = true,
                    sortOrder = 1,
                    supportsVision = true,
                    supportsTools = true,
                    supportsReasoning = true,
                    contextWindow = 1_000_000
                )
            )
        ),
        OpenAiCompatibleProviderSetting(
            id = DEEPSEEK_ID,
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            isBuiltIn = true,
            sortOrder = 3,
            systemPrompt = DEFAULT_SYSTEM_PROMPT,
            models = listOf(
                Model(
                    id = "builtin-deepseek-v4-pro",
                    modelId = "deepseek-v4-pro",
                    displayName = "DeepSeek V4 Pro",
                    isBuiltIn = true,
                    supportsTools = true,
                    supportsReasoning = true,
                    contextWindow = 1_000_000
                ),
                Model(
                    id = "builtin-deepseek-v4-flash",
                    modelId = "deepseek-v4-flash",
                    displayName = "DeepSeek V4 Flash",
                    isBuiltIn = true,
                    sortOrder = 1,
                    supportsTools = true,
                    supportsReasoning = true,
                    contextWindow = 1_000_000
                )
            )
        ),
        OpenAiCompatibleProviderSetting(
            id = SILICONFLOW_ID,
            name = "硅基流动",
            baseUrl = "https://api.siliconflow.cn/v1",
            isBuiltIn = true,
            sortOrder = 4,
            systemPrompt = DEFAULT_SYSTEM_PROMPT
        ),
        OpenAiCompatibleProviderSetting(
            id = OPENROUTER_ID,
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            isBuiltIn = true,
            sortOrder = 5,
            systemPrompt = DEFAULT_SYSTEM_PROMPT
        )
    )

    fun providerById(id: String): ProviderSetting? =
        PROVIDERS.firstOrNull { it.id == id }
}
