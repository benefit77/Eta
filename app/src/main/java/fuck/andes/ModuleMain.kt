package fuck.andes

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

class ModuleMain : XposedModule() {

    private val logger = ModuleLogger(this)
    private var systemServerInstalled = false
    private var systemUiInstalled = false
    private var googleInstalled = false
    private var colorDirectInstalled = false
    private var currentProcessName: String? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        currentProcessName = param.processName
        logger.debug(
            "模块已加载 process=${param.processName}, framework=$frameworkName($frameworkVersionCode), api=$apiVersion"
        )
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        if (systemServerInstalled) return
        systemServerInstalled = true
        SystemServerHooks.install(this, logger, param.classLoader)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        when (param.packageName) {
            ModuleConfig.SYSTEM_UI_PACKAGE -> {
                if (!systemUiInstalled && currentProcessName == ModuleConfig.SYSTEM_UI_PACKAGE) {
                    systemUiInstalled = true
                    SystemUiHooks.install(this, logger, param.classLoader)
                }
            }

            ModuleConfig.GOOGLE_PACKAGE -> {
                if (!googleInstalled && isCurrentPackageProcess(ModuleConfig.GOOGLE_PACKAGE)) {
                    googleInstalled = true
                    GoogleEligibilityHooks.install(this, logger, param.classLoader)
                    GoogleAppHooks.install(this, logger, param.classLoader)
                }
            }

            ModuleConfig.COLOR_DIRECT_PACKAGE -> {
                if (!colorDirectInstalled && isCurrentPackageProcess(ModuleConfig.COLOR_DIRECT_PACKAGE)) {
                    colorDirectInstalled = true
                    ColorDirectHooks.install(this, logger, param.classLoader)
                }
            }
        }
    }

    private fun isCurrentPackageProcess(packageName: String): Boolean {
        val processName = currentProcessName ?: return false
        return processName == packageName || processName.startsWith("$packageName:")
    }
}
