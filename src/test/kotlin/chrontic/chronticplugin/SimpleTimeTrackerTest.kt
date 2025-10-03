package chrontic.chronticplugin

import org.junit.Test
import org.junit.Assert.*

class SimpleTimeTrackerTest {

    @Test
    fun testTimeTrackingLogic() {
        // Test the core logic of time tracking without complex mocks
        val tracker = MockTimeTracker()

        // First call with a ticket should initialize tracking
        tracker.simulateTrackTime("PROJ-123")
        assertEquals("PROJ-123", tracker.getCurrentTicket())
        assertEquals(0, tracker.getTimeEntriesCreated())

        // Second call with same ticket should create time entry
        tracker.simulateTrackTime("PROJ-123")
        assertEquals(1, tracker.getTimeEntriesCreated())

        // Third call should create another entry
        tracker.simulateTrackTime("PROJ-123")
        assertEquals(2, tracker.getTimeEntriesCreated())
    }

    @Test
    fun testTicketSwitching() {
        val tracker = MockTimeTracker()

        // Start with first ticket and create one entry
        tracker.simulateTrackTime("PROJ-123")
        tracker.simulateTrackTime("PROJ-123")
        assertEquals(1, tracker.getTimeEntriesCreated())

        // Switch to different ticket - this should reset tracking
        tracker.simulateTrackTime("PROJ-456")
        assertEquals("PROJ-456", tracker.getCurrentTicket())

        // Should create entry for new ticket on next call
        tracker.simulateTrackTime("PROJ-456")
        assertEquals(2, tracker.getTimeEntriesCreated())
    }

    @Test
    fun testNoTicketHandling() {
        val tracker = MockTimeTracker()

        // Call with no ticket should not create entries
        tracker.simulateTrackTime(null)
        assertEquals(0, tracker.getTimeEntriesCreated())
        assertNull(tracker.getCurrentTicket())

        // Multiple calls with no ticket should still not create entries
        tracker.simulateTrackTime(null)
        tracker.simulateTrackTime(null)
        assertEquals(0, tracker.getTimeEntriesCreated())
    }

    @Test
    fun testBranchNameFallback() {
        val tracker = MockTimeTracker()

        // Test with branch name when no JIRA ticket
        tracker.simulateTrackTimeWithBranch(null, "feature/add-user-authentication")
        assertEquals("feature/add-user-authentication", tracker.getCurrentTicket())
        assertEquals(0, tracker.getTimeEntriesCreated())

        // Second call should create time entry using branch name
        tracker.simulateTrackTimeWithBranch(null, "feature/add-user-authentication")
        assertEquals(1, tracker.getTimeEntriesCreated())
    }

    @Test
    fun testBranchNameWithMissingKeyword() {
        val tracker = MockTimeTracker()

        // Test branch names that contain "missing" - should still work
        tracker.simulateTrackTimeWithBranch(null, "branch_name_missing")
        assertEquals("branch_name_missing", tracker.getCurrentTicket())

        tracker.simulateTrackTimeWithBranch(null, "feature/missing_data_fix")
        assertEquals("feature/missing_data_fix", tracker.getCurrentTicket())
    }

    @Test
    fun testJiraTicketPriority() {
        val tracker = MockTimeTracker()

        // Test that JIRA ticket takes priority over branch name
        tracker.simulateTrackTimeWithBranch("PROJ-123", "feature/PROJ-123-new-feature")
        assertEquals("PROJ-123", tracker.getCurrentTicket()) // Should use JIRA ticket, not branch

        tracker.simulateTrackTimeWithBranch("PROJ-123", "feature/PROJ-123-new-feature")
        assertEquals(1, tracker.getTimeEntriesCreated())
    }

    @Test
    fun testActivityDetection() {
        val tracker = MockTimeTrackerWithActivity()

        // Test inactive project - should not track time
        tracker.setProjectActive(false)
        tracker.simulateTrackTime("PROJ-123")
        assertEquals(0, tracker.getTimeEntriesCreated())

        // Test active project - should track time
        tracker.setProjectActive(true)
        tracker.simulateTrackTime("PROJ-123") // Initialize
        tracker.simulateTrackTime("PROJ-123") // Create entry
        assertEquals(1, tracker.getTimeEntriesCreated())
    }

    @Test
    fun testActivityFluctuations() {
        val tracker = MockTimeTrackerWithActivity()

        // Start active
        tracker.setProjectActive(true)
        tracker.simulateTrackTime("PROJ-123") // Initialize

        // Become inactive - should skip tracking
        tracker.setProjectActive(false)
        tracker.simulateTrackTime("PROJ-123")
        assertEquals(0, tracker.getTimeEntriesCreated())

        // Become active again - should create entry
        tracker.setProjectActive(true)
        tracker.simulateTrackTime("PROJ-123")
        assertEquals(1, tracker.getTimeEntriesCreated())
    }

    @Test
    fun testSwitchingBetweenJiraAndNonJiraBranches() {
        val tracker = MockTimeTracker()

        // Start with JIRA ticket branch
        tracker.simulateTrackTimeWithBranch("PROJ-123", "feature/PROJ-123-feature")
        tracker.simulateTrackTimeWithBranch("PROJ-123", "feature/PROJ-123-feature")
        assertEquals(1, tracker.getTimeEntriesCreated())

        // Switch to non-JIRA branch
        tracker.simulateTrackTimeWithBranch(null, "hotfix/urgent-fix")
        assertEquals("hotfix/urgent-fix", tracker.getCurrentTicket())

        tracker.simulateTrackTimeWithBranch(null, "hotfix/urgent-fix")
        assertEquals(2, tracker.getTimeEntriesCreated())
    }

    // Mock implementation for testing time tracking logic
    private class MockTimeTracker {
        private var lastJiraTicket: String? = null
        private var timeEntriesCreated = 0
        private var hasInitializedTicket = false

        fun simulateTrackTime(jiraTicket: String?) {
            simulateTrackTimeWithBranch(jiraTicket, null)
        }

        fun simulateTrackTimeWithBranch(jiraTicket: String?, branchName: String?) {
            // Use JIRA ticket as priority, fallback to branch name
            val trackingKey = jiraTicket ?: branchName

            if (trackingKey == null) {
                return
            }

            if (lastJiraTicket != trackingKey) {
                // New ticket/branch - initialize tracking
                lastJiraTicket = trackingKey
                hasInitializedTicket = true
                return
            }

            // Same ticket/branch - create time entry
            if (hasInitializedTicket) {
                timeEntriesCreated++
            }
        }

        fun getCurrentTicket(): String? = lastJiraTicket
        fun getTimeEntriesCreated(): Int = timeEntriesCreated
    }

    // Mock implementation with activity detection
    private class MockTimeTrackerWithActivity {
        private var lastJiraTicket: String? = null
        private var timeEntriesCreated = 0
        private var hasInitializedTicket = false
        private var isProjectActive = true

        fun setProjectActive(active: Boolean) {
            isProjectActive = active
        }

        fun simulateTrackTime(jiraTicket: String?) {
            // Check activity first (like real TimeTracker)
            if (!isProjectActive) {
                return
            }

            if (jiraTicket == null) {
                return
            }

            if (lastJiraTicket != jiraTicket) {
                lastJiraTicket = jiraTicket
                hasInitializedTicket = true
                return
            }

            if (hasInitializedTicket) {
                timeEntriesCreated++
            }
        }

        fun getCurrentTicket(): String? = lastJiraTicket
        fun getTimeEntriesCreated(): Int = timeEntriesCreated
    }
}
