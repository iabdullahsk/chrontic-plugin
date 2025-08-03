package clocklytic.clocklyticplugin

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class ClocklyticPlugin : StartupActivity {
    private val logger = Logger.getInstance(ClocklyticPlugin::class.java)
    private val executor = Executors.newSingleThreadScheduledExecutor()

    override fun runActivity(project: Project) {
        logger.info("Starting Clocklytic Plugin")

        val gitService = GitService(project)
        val clocklyticService = ClocklyticService()
        val timeTracker = TimeTracker(gitService, clocklyticService)

        // Start time tracking with 15-minute intervals (configurable)
        executor.scheduleAtFixedRate({
            try {
                timeTracker.trackTime()
            } catch (e: Exception) {
                logger.error("Error during time tracking", e)
            }
        }, 0, 15, TimeUnit.MINUTES)

        logger.info("Clocklytic Plugin started successfully")
    }
}
