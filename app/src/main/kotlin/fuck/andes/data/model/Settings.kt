package fuck.andes.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val providers: List<ProviderSetting> = emptyList(),
    val selectedProviderId: String? = null,
    val selectedModelId: String? = null,
    val legacyMigrationCompleted: Boolean = false
)
