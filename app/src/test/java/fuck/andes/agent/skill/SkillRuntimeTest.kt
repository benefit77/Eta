package fuck.andes.agent.skill

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SkillRuntimeTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun seedBuiltinSkillsDoesNotOverwriteExistingBuiltinData() {
        val service = SkillIndexService(
            context = RuntimeEnvironment.getApplication(),
            skillsRoot = temporaryFolder.newFolder("skills"),
        )
        service.seedBuiltinSkillsIfNeeded()
        val errorsFile = File(
            temporaryFolder.root,
            "skills/self-improving-agent/data/ERRORS.md",
        )
        errorsFile.parentFile?.mkdirs()
        errorsFile.writeText("preserve existing learning\n")

        service.seedBuiltinSkillsIfNeeded()

        assertEquals("preserve existing learning\n", errorsFile.readText())
    }
}
