package fuck.andes.data.repository

internal object ModelCapabilityRegistry {
    fun supportsVision(modelId: String, ownedBy: String = ""): Boolean {
        val id = modelId.lowercase()
        val owner = ownedBy.lowercase()
        return when {
            owner.contains("anthropic") -> true
            id.contains("gpt-5") -> true
            id.contains("gpt-4o") -> true
            id.contains("vision") -> true
            id.contains("qwen") && (id.contains("vl") || id.contains("3.7")) -> true
            id.contains("gemini") -> true
            id.contains("llava") -> true
            else -> false
        }
    }

    fun supportsTools(modelId: String, ownedBy: String = ""): Boolean {
        val id = modelId.lowercase()
        val owner = ownedBy.lowercase()
        return when {
            owner.contains("anthropic") -> true
            id.contains("gpt-") -> true
            id.contains("claude") -> true
            id.contains("qwen") -> true
            id.contains("deepseek") -> true
            id.contains("gemini") -> true
            id.contains("llama") && (id.contains("70b") || id.contains("405b")) -> true
            else -> false
        }
    }

    fun supportsReasoning(modelId: String, ownedBy: String = ""): Boolean {
        val id = modelId.lowercase()
        val owner = ownedBy.lowercase()
        return when {
            owner.contains("anthropic") -> true
            id.contains("gpt-5") -> true
            id.contains("claude") -> true
            id.contains("qwen3") -> true
            id.contains("deepseek-v4") -> true
            id.contains("reasoner") -> true
            id.contains("deepseek-r1") -> true
            id.contains("o1") -> true
            id.contains("o3") -> true
            id.contains("qwq") -> true
            else -> false
        }
    }

    fun contextWindow(modelId: String, ownedBy: String = ""): Int? {
        val id = modelId.lowercase()
        val owner = ownedBy.lowercase()
        return when {
            owner.contains("anthropic") -> 1_000_000
            id.contains("qwen3.7") -> 1_000_000
            id.contains("deepseek-v4") -> 1_000_000
            else -> null
        }
    }
}
