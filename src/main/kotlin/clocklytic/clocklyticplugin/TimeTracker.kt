package clocklytic.clocklyticplugin

import com.intellij.openapi.diagnostic.Logger
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TimeTracker(
    private val gitService: GitService,
    private val clocklyticService: ClocklyticService,
    private val activityDetector: ActivityDetector,
    private val projectName: String
) {
    private val logger = Logger.getInstance(TimeTracker::class.java)
    private var lastJiraTicket: String? = null
    private var lastTrackingTime: LocalDateTime? = null

    fun trackTime() {
        // First check if the project is actively being worked on
        if (!activityDetector.isProjectActivelyBeingWorkedOn()) {
            logger.info("Project not actively being worked on - skipping time tracking")
            return
        }

        val currentJiraTicket = gitService.getCurrentJiraTicket()
        val currentBranchName = gitService.getCurrentBranchName()
        val currentTime = LocalDateTime.now()

        // Use branch name as identifier if no JIRA ticket found
        val trackingKey = currentJiraTicket ?: currentBranchName

        if (trackingKey == null) {
            logger.info("No JIRA ticket or branch name found, skipping time tracking")
            return
        }

        // If this is the first time tracking or we switched to a different branch/ticket
        if (lastJiraTicket == null || lastJiraTicket != trackingKey) {
            logger.info("Starting time tracking for: $trackingKey")
            lastJiraTicket = trackingKey
            lastTrackingTime = currentTime
            return
        }

        // Calculate duration since last tracking
        val lastTime = lastTrackingTime ?: return
        val durationMinutes = ChronoUnit.MINUTES.between(lastTime, currentTime)

        if (durationMinutes >= 1) { // Only track if at least 1 minute has passed
            logger.info("Attempting to track $durationMinutes minutes for: $trackingKey (Last activity: ${activityDetector.getMinutesSinceLastActivity()} minutes ago)")

            val success = clocklyticService.createTimeEntry(currentJiraTicket, currentBranchName, projectName, durationMinutes.toInt())

            if (success) {
                lastTrackingTime = currentTime
                logger.info("Successfully tracked $durationMinutes minutes for: $trackingKey")
            } else {
                logger.warn("Failed to track time for: $trackingKey")
            }
        }
    }
}
