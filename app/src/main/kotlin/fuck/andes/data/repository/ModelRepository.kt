package fuck.andes.data.repository

import fuck.andes.data.datastore.SettingsDataStore
import fuck.andes.data.model.Model
import fuck.andes.data.model.withModels
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal object ModelRepository {
    fun modelsByProviderFlow(providerId: String): Flow<List<Model>> =
        allModelsByProviderFlow(providerId).map { models ->
            models.filter { it.isEnabled }.sortedBy { it.sortOrder }
        }

    fun allModelsByProviderFlow(providerId: String): Flow<List<Model>> =
        SettingsDataStore.settingsFlow().map { settings ->
            settings.providers.firstOrNull { it.id == providerId }
                ?.models
                ?.sortedBy { it.sortOrder }
                .orEmpty()
        }

    suspend fun modelsByProvider(providerId: String): List<Model> =
        SettingsDataStore.settings().providers.firstOrNull { it.id == providerId }
            ?.models
            ?.sortedBy { it.sortOrder }
            .orEmpty()

    suspend fun addModel(providerId: String, model: Model) {
        updateProviderModels(providerId) { models ->
            val nextOrder = (models.maxOfOrNull { it.sortOrder } ?: -1) + 1
            models + model.copy(sortOrder = nextOrder)
        }
    }

    suspend fun updateModel(providerId: String, model: Model) {
        updateProviderModels(providerId) { models ->
            models.map { if (it.id == model.id) model else it }
        }
    }

    suspend fun deleteModel(providerId: String, modelId: String) {
        updateProviderModels(providerId) { models ->
            models.filterNot { it.id == modelId }
        }
    }

    suspend fun replaceModelsForProvider(providerId: String, newModels: List<Model>) {
        updateProviderModels(providerId) {
            newModels.mapIndexed { index, model ->
                model.copy(sortOrder = index)
            }
        }
    }

    suspend fun reorderModels(providerId: String, ids: List<String>) {
        updateProviderModels(providerId) { models ->
            val byId = models.associateBy { it.id }
            val reordered = ids.mapNotNull { byId[it] }
                .mapIndexed { index, model -> model.copy(sortOrder = index) }
            val rest = models.filterNot { it.id in ids }
            reordered + rest
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    private suspend fun updateProviderModels(
        providerId: String,
        transform: (List<Model>) -> List<Model>
    ) {
        SettingsDataStore.updateSettings { settings ->
            settings.copy(
                providers = settings.providers.map { provider ->
                    if (provider.id == providerId) {
                        provider.withModels(transform(provider.models).sortedBy { it.sortOrder })
                    } else {
                        provider
                    }
                }
            ).let(ProviderRepository::repair)
        }
    }
}
