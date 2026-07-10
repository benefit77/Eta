package fuck.andes.agent.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgentRuntimeResultStoreTest {
    @Test
    fun acknowledgementBeforeCompletionPreventsLateResultWriteBack() {
        val context = RuntimeEnvironment.getApplication()
        val runId = "ack-before-add-${System.nanoTime()}"
        val completedRun = AgentRuntimeWire.CompletedRun(
            handoff = AgentRuntimeWire.EntryHandoff(
                id = runId,
                source = "breeno",
                payload = "{}",
            ),
            result = AgentRuntimeWire.RunResult(
                runId = runId,
                ok = true,
                content = "obsolete result",
            ),
            createdAt = System.currentTimeMillis(),
        )

        AgentRuntimeResultStore.remove(context, runId)

        assertFalse(AgentRuntimeResultStore.add(context, completedRun))
        assertTrue(AgentRuntimeResultStore.list(context).none { it.result.runId == runId })
    }
}
