package fuck.andes.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Model(
    val id: String,
    val modelId: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val sortOrder: Int = 0,
    val supportsVision: Boolean = false,
    val supportsTools: Boolean = false,
    val supportsReasoning: Boolean = false,
    val contextWindow: Int? = null,
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBody: List<CustomBody> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
