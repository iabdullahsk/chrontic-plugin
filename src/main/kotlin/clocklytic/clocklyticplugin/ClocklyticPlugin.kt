package clocklytic.clocklyticplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.components.Service
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ClocklyticPlugin : ProjectActivity {
    private val logger = Logger.getInstance(ClocklyticPlugin::class.java)

    override suspend fun execute(project: Project) {
        val settings = ClocklyticSettings.getInstance()

        logger.info("=== Clocklytic Plugin Execute Called for project: ${project.name} ===")
        logger.info("Auto-tracking enabled: ${settings.enableAutoTracking}")
        logger.info("API Base URL: ${settings.apiBaseUrl}")
        logger.info("Tracking interval: ${settings.trackingIntervalMinutes} minutes")

        if (!settings.enableAutoTracking) {
            logger.info("Auto-tracking is disabled in settings")
            return
        }

        logger.info("Starting Clocklytic Plugin initialization...")

        val gitService = GitService(project)
        val clocklyticService = ClocklyticService()
        val activityDetector = ActivityDetector(project)
        val timeTracker = TimeTracker(gitService, clocklyticService, activityDetector)

        // Test git service immediately
        val currentTicket = gitService.getCurrentJiraTicket()
        logger.info("Initial JIRA ticket detection: $currentTicket")

        val executor = AppExecutorUtil.createBoundedScheduledExecutorService(
            "Clocklytic Time Tracker", 1
        )

        // Start time tracking with configurable intervals
        val scheduledTask = executor.scheduleWithFixedDelay({
            try {
                logger.info("=== Clocklytic Time Tracker - Scheduled execution ===")
                timeTracker.trackTime()
                logger.info("=== Clocklytic Time Tracker - Execution completed ===")
            } catch (e: Exception) {
                logger.error("Error during time tracking", e)
            }
        }, 0, settings.trackingIntervalMinutes.toLong(), TimeUnit.MINUTES)

        // Register disposal to cleanup resources when project is closed
        project.service<TimeTrackerDisposable>().setup(scheduledTask, executor)

        logger.info("Clocklytic Plugin started successfully with ${settings.trackingIntervalMinutes} minute intervals")
        logger.info("=== Clocklytic Plugin Initialization Complete ===")
    }
}

// Service to handle cleanup when project is disposed
@Service(Service.Level.PROJECT)
class TimeTrackerDisposable : Disposable {
    private var scheduledTask: ScheduledFuture<*>? = null
    private var executor: ScheduledExecutorService? = null

    fun setup(task: ScheduledFuture<*>, exec: ScheduledExecutorService) {
        scheduledTask = task
        executor = exec
    }

    override fun dispose() {
        scheduledTask?.cancel(true)
        executor?.shutdown()
    }
}
