package fuck.andes.agent.runtime

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores completed runs until an entry adapter confirms that the result was shown.
 *
 * This is intentionally process-persistent: Android may kill the entry process while
 * the agent is operating another app, so the final answer must survive that process death.
 */
internal object AgentRuntimeResultStore {
    private const val PREFS_NAME = "agent_runtime_results"
    private const val KEY_PENDING = "pending"
    private const val MAX_PENDING = 8
    private const val MAX_AGE_MS = 12L * 60L * 60L * 1000L

    fun add(context: Context, completedRun: AgentRuntimeWire.CompletedRun) {
        val runs = list(context)
            .filterNot { it.result.runId == completedRun.result.runId } + completedRun
        write(context, prune(runs))
    }

    fun list(context: Context): List<AgentRuntimeWire.CompletedRun> {
        val raw = prefs(context).getString(KEY_PENDING, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        val now = System.currentTimeMillis()
        var decodedLength = 0
        val runs = runCatching {
            val array = JSONArray(raw)
            decodedLength = array.length()
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let(::fromJson)
            }
        }.getOrDefault(emptyList())
            .filter { now - it.createdAt <= MAX_AGE_MS }
            .sortedBy { it.createdAt }
        if (runs.size != decodedLength) {
            write(context, prune(runs))
        }
        return runs
    }

    fun remove(context: Context, runId: String) {
        if (runId.isBlank()) return
        write(context, list(context).filterNot { it.result.runId == runId })
    }

    private fun prune(runs: List<AgentRuntimeWire.CompletedRun>): List<AgentRuntimeWire.CompletedRun> {
        val now = System.currentTimeMillis()
        return runs
            .filter { now - it.createdAt <= MAX_AGE_MS }
            .sortedBy { it.createdAt }
            .takeLast(MAX_PENDING)
    }

    private fun write(context: Context, runs: List<AgentRuntimeWire.CompletedRun>) {
        val array = JSONArray()
        runs.forEach { array.put(toJson(it)) }
        prefs(context).edit().putString(KEY_PENDING, array.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun toJson(run: AgentRuntimeWire.CompletedRun): JSONObject =
        JSONObject()
            .put("createdAt", run.createdAt)
            .put(
                "handoff",
                JSONObject()
                    .put("id", run.handoff.id)
                    .put("source", run.handoff.source)
                    .put("payload", run.handoff.payload)
            )
            .put(
                "result",
                JSONObject()
                    .put("runId", run.result.runId)
                    .put("ok", run.result.ok)
                    .put("content", run.result.content)
                    .put("reasoningContent", run.result.reasoningContent)
                    .put("error", run.result.error)
            )

    private fun fromJson(json: JSONObject): AgentRuntimeWire.CompletedRun? =
        runCatching {
            val handoff = json.getJSONObject("handoff")
            val result = json.getJSONObject("result")
            AgentRuntimeWire.CompletedRun(
                createdAt = json.optLong("createdAt"),
                handoff = AgentRuntimeWire.EntryHandoff(
                    id = handoff.optString("id"),
                    source = handoff.optString("source"),
                    payload = handoff.optString("payload")
                ),
                result = AgentRuntimeWire.RunResult(
                    runId = result.optString("runId").ifBlank { json.optString("runId") },
                    ok = result.optBoolean("ok"),
                    content = result.optString("content"),
                    error = result.optString("error").ifBlank { null },
                    reasoningContent = result.optString("reasoningContent")
                        .ifBlank { result.optString("reasoning_content") }
                )
            )
        }.getOrNull()
}
