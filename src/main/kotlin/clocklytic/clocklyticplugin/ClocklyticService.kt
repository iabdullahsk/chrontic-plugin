package clocklytic.clocklyticplugin

import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class TimeEntryRequest(
    val jiraTicket: String,
    val startTime: String,
    val duration: Int, // in minutes
    val description: String = "Auto-tracked via IntelliJ plugin"
)

class ClocklyticService {
    private val logger = Logger.getInstance(ClocklyticService::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    // TODO: Make these configurable via plugin settings
    private val baseUrl = "http://localhost:8080" // Your Clocklytic API base URL
    private val apiKey = "your-api-key" // Your API key

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun createTimeEntry(jiraTicket: String, durationMinutes: Int): Boolean {
        try {
            val timeEntry = TimeEntryRequest(
                jiraTicket = jiraTicket,
                startTime = LocalDateTime.now().minusMinutes(durationMinutes.toLong())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                duration = durationMinutes
            )

            val requestBody = json.encodeToString(timeEntry)

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/time-entries"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiKey")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                logger.info("Time entry created successfully for ticket: $jiraTicket, duration: $durationMinutes minutes")
                return true
            } else {
                logger.error("Failed to create time entry. Status: ${response.statusCode()}, Body: ${response.body()}")
                return false
            }

        } catch (e: Exception) {
            logger.error("Error creating time entry for ticket: $jiraTicket", e)
            return false
        }
    }
}
