package clocklytic.clocklyticplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Document
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.application.ApplicationManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ActivityDetector(private val project: Project) {
    private val logger = Logger.getInstance(ActivityDetector::class.java)
    private var lastActivityTime: LocalDateTime? = null
    private var lastFocusCheckTime: LocalDateTime = LocalDateTime.now()

    // Configurable activity timeout (default: 5 minutes)
    private val activityTimeoutMinutes: Long = 5

    init {
        setupActivityListeners()
    }

    private fun setupActivityListeners() {
        // Listen for document changes (typing, editing)
        project.messageBus.connect().subscribe(
            FileDocumentManagerListener.TOPIC,
            object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: Document) {
                    recordActivity("Document saving")
                }
            }
        )

        // Listen for user actions (clicks, shortcuts, etc.)
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            AnActionListener.TOPIC,
            object : AnActionListener {
                override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
                    // Filter out background/automatic actions
                    if (event.project == project && !isBackgroundAction(action)) {
                        recordActivity("User action: ${action.javaClass.simpleName}")
                    }
                }
            }
        )
    }

    private fun isBackgroundAction(action: AnAction): Boolean {
        val actionName = action.javaClass.simpleName.lowercase()
        return actionName.contains("background") ||
               actionName.contains("auto") ||
               actionName.contains("daemon") ||
               actionName.contains("update")
    }

    private fun recordActivity(activityType: String) {
        lastActivityTime = LocalDateTime.now()
        logger.debug("Activity detected: $activityType")
    }

    fun isProjectActivelyBeingWorkedOn(): Boolean {
        val now = LocalDateTime.now()

        // Check window focus periodically (not too frequently to avoid performance impact)
        if (ChronoUnit.MINUTES.between(lastFocusCheckTime, now) >= 1) {
            checkWindowFocus()
            lastFocusCheckTime = now
        }

        // If no activity detected yet, consider it active (first run)
        if (lastActivityTime == null) {
            return isWindowFocused()
        }

        // Check if there was recent activity within the timeout period
        val minutesSinceLastActivity = ChronoUnit.MINUTES.between(lastActivityTime, now)
        val isRecentlyActive = minutesSinceLastActivity <= activityTimeoutMinutes

        // Combine activity check with window focus
        val isWindowActive = isWindowFocused()

        val isActive = isRecentlyActive && isWindowActive

        logger.debug("Activity check - Recent activity: $isRecentlyActive (${minutesSinceLastActivity}min ago), Window focused: $isWindowActive, Overall active: $isActive")

        return isActive
    }

    private fun checkWindowFocus() {
        // Update focus status but don't use it to record activity
        // (we don't want just having the window open to count as activity)
        val focused = isWindowFocused()
        if (focused) {
            logger.debug("IDE window is focused")
        }
    }

    private fun isWindowFocused(): Boolean {
        return try {
            val frame = WindowManager.getInstance().getFrame(project)
            frame?.isActive == true
        } catch (e: Exception) {
            logger.warn("Could not determine window focus state", e)
            true // Assume active if we can't determine
        }
    }

    fun getLastActivityTime(): LocalDateTime? = lastActivityTime

    fun getMinutesSinceLastActivity(): Long {
        val lastActivity = lastActivityTime ?: return Long.MAX_VALUE
        return ChronoUnit.MINUTES.between(lastActivity, LocalDateTime.now())
    }
}
