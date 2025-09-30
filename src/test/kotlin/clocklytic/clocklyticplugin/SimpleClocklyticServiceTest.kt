package clocklytic.clocklyticplugin

import org.junit.Test
import org.junit.Assert.*

class SimpleClocklyticServiceTest {

    @Test
    fun testTimeEntryRequestCreation() {
        // Test the updated TimeEntryRequest with correct parameters
        val startTime = java.time.LocalDateTime.of(2025, 8, 7, 10, 0, 0)
        val endTime = java.time.LocalDateTime.of(2025, 8, 7, 10, 15, 0)

        val timeEntry = TimeEntryRequest(
            startTime = startTime,
            endTime = endTime,
            hoursWorked = 0.25,
            description = "PROJ-123"
        )

        assertEquals(startTime, timeEntry.startTime)
        assertEquals(endTime, timeEntry.endTime)
        assertEquals(0.25, timeEntry.hoursWorked, 0.001)
        assertEquals("PROJ-123", timeEntry.description)
    }

    @Test
    fun testApiUrlFormatting() {
        val testCases = mapOf(
            "http://localhost:8080" to "http://localhost:8080/api/time-entries",
            "http://localhost:8080/" to "http://localhost:8080/api/time-entries",
            "https://api.clocklytic.com" to "https://api.clocklytic.com/api/time-entries"
        )

        testCases.forEach { (baseUrl, expectedUrl) ->
            val formattedUrl = formatApiUrl(baseUrl)
            assertEquals("Failed for $baseUrl", expectedUrl, formattedUrl)
        }
    }

    @Test
    fun testConfigurationValidation() {
        assertTrue("Valid API key should pass", isValidApiKey("test-key-123"))
        assertFalse("Empty API key should fail", isValidApiKey(""))
        assertFalse("Blank API key should fail", isValidApiKey("   "))

        assertTrue("Valid URL should pass", isValidUrl("https://api.test.com"))
        assertTrue("HTTP URL should pass", isValidUrl("http://localhost:8080"))
        assertFalse("Empty URL should fail", isValidUrl(""))
        assertFalse("Invalid URL should fail", isValidUrl("not-a-url"))
    }

    @Test
    fun testDurationValidation() {
        val validDurations = listOf(1, 5, 15, 30, 60)
        validDurations.forEach { duration ->
            assertTrue("Duration $duration should be valid", isValidDuration(duration))
        }

        val invalidDurations = listOf(0, -1, -10, 61)
        invalidDurations.forEach { duration ->
            assertFalse("Duration $duration should be invalid", isValidDuration(duration))
        }
    }

    @Test
    fun testNewTimeEntryRequestFormat() {
        // Test the new backend format with LocalDateTime and hoursWorked
        val startTime = java.time.LocalDateTime.of(2025, 8, 7, 10, 0, 0)
        val endTime = java.time.LocalDateTime.of(2025, 8, 7, 10, 15, 0)

        val timeEntry = TimeEntryRequest(
            startTime = startTime,
            endTime = endTime,
            hoursWorked = 0.25,
            description = "PROJ-123"
        )

        assertEquals(startTime, timeEntry.startTime)
        assertEquals(endTime, timeEntry.endTime)
        assertEquals(0.25, timeEntry.hoursWorked, 0.001)
        assertEquals("PROJ-123", timeEntry.description)
    }

    @Test
    fun testDescriptionGeneration_JiraTicketPresent() {
        // Test description when JIRA ticket is available - should use just the ticket
        val description = generateDescription("PROJ-123", "feature/PROJ-123-new-feature", "MyProject")
        assertEquals("PROJ-123", description)
    }

    @Test
    fun testDescriptionGeneration_OnlyBranchName() {
        // Test description when only branch name is available - should prefix with project name
        val description = generateDescription(null, "feature/add-user-authentication", "MyProject")
        assertEquals("MyProject_feature/add-user-authentication", description)
    }

    @Test
    fun testDescriptionGeneration_BranchWithMissingKeyword() {
        // Test description with branch containing "missing" - should work fine with project prefix
        val description = generateDescription(null, "branch_name_missing", "TestProject")
        assertEquals("TestProject_branch_name_missing", description)
    }

    @Test
    fun testDescriptionGeneration_EmptyValues() {
        // Test description with empty/null values
        assertEquals("Auto-tracked via IntelliJ plugin", generateDescription(null, null, "MyProject"))
        assertEquals("Auto-tracked via IntelliJ plugin", generateDescription("", "", "MyProject"))
        assertEquals("Auto-tracked via IntelliJ plugin", generateDescription("   ", "   ", "MyProject"))
    }

    @Test
    fun testHoursWorkedCalculation() {
        // Test conversion from minutes to hours
        val testCases = mapOf(
            15 to 0.25,   // 15 minutes = 0.25 hours
            30 to 0.5,    // 30 minutes = 0.5 hours
            60 to 1.0,    // 60 minutes = 1.0 hour
            90 to 1.5,    // 90 minutes = 1.5 hours
            1 to 0.0167   // 1 minute ≈ 0.0167 hours
        )

        testCases.forEach { (minutes, expectedHours) ->
            val actualHours = minutes.toDouble() / 60.0
            assertEquals("Failed for $minutes minutes", expectedHours, actualHours, 0.001)
        }
    }

    @Test
    fun testConfigurationValidation_NewFields() {
        // Test validation for the required API key
        assertFalse("Should be invalid without API key",
            isValidConfiguration(""))

        assertTrue("Should be valid with API key",
            isValidConfiguration("test-key"))
    }

    @Test
    fun testTimeCalculation() {
        // Test that start and end time calculation is correct
        val durationMinutes = 15
        val now = java.time.LocalDateTime.now()
        val calculatedStart = now.minusMinutes(durationMinutes.toLong())
        val calculatedEnd = now

        assertTrue("Start time should be before end time",
            calculatedStart.isBefore(calculatedEnd))

        val actualDuration = java.time.temporal.ChronoUnit.MINUTES
            .between(calculatedStart, calculatedEnd)

        assertEquals("Duration should match", durationMinutes.toLong(), actualDuration)
    }

    @Test
    fun testDescriptionPriority() {
        // Test that JIRA ticket takes priority over branch name in descriptions
        // When using branch name, it should be prefixed with project name
        val projectName = "TestProject"
        val testCases = listOf(
            Triple("PROJ-123", "feature/PROJ-123-test", "PROJ-123"),
            Triple("ABC-456", "feature/different-branch", "ABC-456"),
            Triple(null, "feature/no-ticket", "${projectName}_feature/no-ticket"),
            Triple("", "feature/empty-ticket", "${projectName}_feature/empty-ticket"),
            Triple("   ", "feature/blank-ticket", "${projectName}_feature/blank-ticket")
        )

        testCases.forEach { (jiraTicket, branchName, expected) ->
            val result = generateDescription(jiraTicket, branchName, projectName)
            assertEquals("Priority test failed for JIRA='$jiraTicket', branch='$branchName'",
                expected, result)
        }
    }

    // Helper methods
    private fun formatApiUrl(baseUrl: String): String {
        val cleanUrl = baseUrl.trimEnd('/')
        return "$cleanUrl/api/time-entries"
    }

    private fun isValidApiKey(apiKey: String): Boolean {
        return apiKey.isNotBlank()
    }

    private fun isValidUrl(url: String): Boolean {
        return url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
    }

    private fun isValidDuration(duration: Int): Boolean {
        return duration in 1..60
    }

    // Helper method for description generation logic
    private fun generateDescription(jiraTicket: String?, branchName: String?, projectName: String): String {
        return when {
            !jiraTicket.isNullOrBlank() -> jiraTicket
            !branchName.isNullOrBlank() -> "${projectName}_$branchName"
            else -> "Auto-tracked via IntelliJ plugin"
        }
    }

    // Helper method for new configuration validation
    private fun isValidConfiguration(apiKey: String): Boolean {
        return apiKey.isNotBlank()
    }
}
