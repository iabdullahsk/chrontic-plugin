package chrontic.chronticplugin

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ChronticSettingsTest {

    private lateinit var settings: ChronticSettings

    @Before
    fun setUp() {
        settings = ChronticSettings()
    }

    @Test
    fun testDefaultSettings() {
        assertEquals("Default API base URL should be localhost",
            "http://localhost:8086", settings.apiBaseUrl)
        assertEquals("Default API key should be empty", "", settings.apiKey)
        assertEquals("Default tracking interval should be 15 minutes",
            15, settings.trackingIntervalMinutes)
        assertTrue("Auto tracking should be enabled by default",
            settings.enableAutoTracking)
        assertEquals("Default JIRA regex should match standard pattern",
            "([A-Z]{2,10}-\\d+)", settings.jiraTicketRegex)
    }

    @Test
    fun testSettingsModification() {
        // Test API base URL
        settings.apiBaseUrl = "https://api.chrontic.com"
        assertEquals("https://api.chrontic.com", settings.apiBaseUrl)

        // Test API key
        settings.apiKey = "test-api-key-123"
        assertEquals("test-api-key-123", settings.apiKey)

        // Test tracking interval
        settings.trackingIntervalMinutes = 30
        assertEquals(30, settings.trackingIntervalMinutes)

        // Test auto tracking toggle
        settings.enableAutoTracking = false
        assertFalse(settings.enableAutoTracking)

        // Test custom JIRA regex
        settings.jiraTicketRegex = "(CUSTOM-\\d{4})"
        assertEquals("(CUSTOM-\\d{4})", settings.jiraTicketRegex)
    }

    @Test
    fun testTrackingIntervalBounds() {
        // Test valid intervals
        val validIntervals = listOf(1, 5, 15, 30, 60)
        validIntervals.forEach { interval ->
            settings.trackingIntervalMinutes = interval
            assertEquals("Interval $interval should be accepted",
                interval, settings.trackingIntervalMinutes)
        }
    }

    @Test
    fun testApiUrlValidation() {
        val validUrls = listOf(
            "http://localhost:8080",
            "https://api.chrontic.com",
            "http://192.168.1.100:3000",
            "https://chrontic.example.com/api"
        )

        validUrls.forEach { url ->
            settings.apiBaseUrl = url
            assertEquals("URL $url should be accepted", url, settings.apiBaseUrl)
        }
    }

    @Test
    fun testJiraRegexPatterns() {
        val regexPatterns = mapOf(
            "([A-Z]{2,10}-\\d+)" to listOf("PROJ-123", "ABC-456"),
            "(TICKET-\\d{4})" to listOf("TICKET-1234", "TICKET-5678"),
            "([A-Z]+-\\d+)" to listOf("A-1", "PROJECT-999")
        )

        regexPatterns.forEach { (pattern, testCases) ->
            settings.jiraTicketRegex = pattern
            assertEquals("Pattern should be set", pattern, settings.jiraTicketRegex)

            // Test that pattern actually works
            val compiledPattern = java.util.regex.Pattern.compile(pattern)
            testCases.forEach { testCase ->
                assertTrue("Pattern $pattern should match $testCase",
                    compiledPattern.matcher(testCase).find())
            }
        }
    }

    @Test
    fun testSettingsStatePersistence() {
        // Simulate state save/load
        val originalSettings = ChronticSettings().apply {
            apiBaseUrl = "https://custom.api.com"
            apiKey = "custom-key"
            trackingIntervalMinutes = 45
            enableAutoTracking = false
            jiraTicketRegex = "(CUSTOM-\\d+)"
        }

        val newSettings = ChronticSettings().apply {
            apiBaseUrl = originalSettings.apiBaseUrl
            apiKey = originalSettings.apiKey
            trackingIntervalMinutes = originalSettings.trackingIntervalMinutes
            enableAutoTracking = originalSettings.enableAutoTracking
            jiraTicketRegex = originalSettings.jiraTicketRegex
        }

        assertEquals(originalSettings.apiBaseUrl, newSettings.apiBaseUrl)
        assertEquals(originalSettings.apiKey, newSettings.apiKey)
        assertEquals(originalSettings.trackingIntervalMinutes, newSettings.trackingIntervalMinutes)
        assertEquals(originalSettings.enableAutoTracking, newSettings.enableAutoTracking)
        assertEquals(originalSettings.jiraTicketRegex, newSettings.jiraTicketRegex)
    }
}
