package clocklytic.clocklyticplugin

import com.intellij.openapi.diagnostic.Logger
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TimeTracker(
    private val gitService: GitService,
    private val clocklyticService: ClocklyticService
) {
    private val logger = Logger.getInstance(TimeTracker::class.java)
    private var lastJiraTicket: String? = null
    private var lastTrackingTime: LocalDateTime? = null

    fun trackTime() {
        val currentJiraTicket = gitService.getCurrentJiraTicket()
        val currentTime = LocalDateTime.now()

        if (currentJiraTicket == null) {
            logger.info("No JIRA ticket found in current branch, skipping time tracking")
            return
        }

        // If this is the first time tracking or we switched to a different ticket
        if (lastJiraTicket == null || lastJiraTicket != currentJiraTicket) {
            logger.info("Starting time tracking for new ticket: $currentJiraTicket")
            lastJiraTicket = currentJiraTicket
            lastTrackingTime = currentTime
            return
        }

        // Calculate duration since last tracking
        val lastTime = lastTrackingTime ?: return
        val durationMinutes = ChronoUnit.MINUTES.between(lastTime, currentTime)

        if (durationMinutes >= 1) { // Only track if at least 1 minute has passed
            val success = clocklyticService.createTimeEntry(currentJiraTicket, durationMinutes.toInt())

            if (success) {
                lastTrackingTime = currentTime
                logger.info("Successfully tracked $durationMinutes minutes for ticket: $currentJiraTicket")
            } else {
                logger.warn("Failed to track time for ticket: $currentJiraTicket")
            }
        }
    }
}
