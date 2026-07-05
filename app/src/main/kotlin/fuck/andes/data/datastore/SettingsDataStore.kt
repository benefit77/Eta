package fuck.andes.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fuck.andes.data.model.Settings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object SettingsDataStore {
    private const val STORE_NAME = "fuck_andes_settings"

    private val SETTINGS_JSON = stringPreferencesKey("settings_json")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)

    @Volatile
    private lateinit var dataStore: DataStore<Preferences>

    fun init(context: Context) {
        if (!::dataStore.isInitialized) {
            dataStore = context.applicationContext.dataStore
        }
    }

    fun settingsFlow(): Flow<Settings> {
        ensureInitialized()
        return dataStore.data
            .catch { cause ->
                if (cause is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw cause
                }
            }
            .map { prefs ->
                prefs[SETTINGS_JSON]?.let(::decodeSettings) ?: Settings()
            }
    }

    suspend fun settings(): Settings = settingsFlow().first()

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        ensureInitialized()
        dataStore.edit { prefs ->
            val current = prefs[SETTINGS_JSON]?.let(::decodeSettings) ?: Settings()
            prefs[SETTINGS_JSON] = json.encodeToString(transform(current))
        }
    }

    fun selectedProviderIdFlow(): Flow<String?> =
        settingsFlow().map { it.selectedProviderId }

    fun selectedModelIdFlow(): Flow<String?> =
        settingsFlow().map { it.selectedModelId }

    suspend fun setSelectedProviderId(id: String?) {
        updateSettings { it.copy(selectedProviderId = id) }
    }

    suspend fun setSelectedModelId(id: String?) {
        updateSettings { it.copy(selectedModelId = id) }
    }

    fun encode(settings: Settings): String = json.encodeToString(settings)

    fun decode(raw: String): Settings = decodeSettings(raw)

    private fun decodeSettings(raw: String): Settings =
        runCatching { json.decodeFromString<Settings>(raw) }.getOrDefault(Settings())

    private fun ensureInitialized() {
        check(::dataStore.isInitialized) {
            "SettingsDataStore.init(context) must be called in Application.onCreate()"
        }
    }
}
