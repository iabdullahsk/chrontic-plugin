package chrontic.chronticplugin

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
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
    @Serializable(with = LocalDateTimeSerializer::class)
    val startTime: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val endTime: LocalDateTime,
    val hoursWorked: Double, // Using Double instead of BigDecimal for JSON serialization
    val description: String
)

// Custom serializer for LocalDateTime
object LocalDateTimeSerializer : kotlinx.serialization.KSerializer<LocalDateTime> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("LocalDateTime", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

class ChronticService {
    private val logger = Logger.getInstance(ChronticService::class.java)
    private val settings = ChronticSettings.getInstance()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun createTimeEntry(jiraTicket: String?, branchName: String?, projectName: String, durationMinutes: Int): Boolean {
        if (settings.apiKey.isBlank()) {
            logger.warn("API key not configured. Please configure the plugin settings.")
            return false
        }

        try {
            val startTime = LocalDateTime.now().minusMinutes(durationMinutes.toLong())
            val endTime = LocalDateTime.now()

            // Create description based on available information
            // If using branch name, prefix it with project name
            val description = when {
                !jiraTicket.isNullOrBlank() -> jiraTicket
                !branchName.isNullOrBlank() -> "${projectName}_$branchName"
                else -> "Auto-tracked via IntelliJ plugin: branch name could not be determined"
            }

            val timeEntry = TimeEntryRequest(
                startTime = startTime,
                endTime = endTime,
                hoursWorked = durationMinutes.toDouble() / 60.0,
                description = description
            )

            val requestBody = json.encodeToString(timeEntry)
            logger.info("Creating time entry with description: $description")

            val request = HttpRequest.newBuilder()
                .uri(URI.create("${settings.apiBaseUrl}/api/time-entries/plugin"))
                .header("Content-Type", "application/json")
                .header("X-API-Key", settings.apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                logger.info("Time entry created successfully. Duration: $durationMinutes minutes, Description: $description")

                // Show notification to user
                showNotification(
                    "Time Entry Recorded",
                    "Tracked $durationMinutes min for: $description",
                    NotificationType.INFORMATION
                )

                return true
            } else {
                logger.error("Failed to create time entry. Status: ${response.statusCode()}, Body: ${response.body()}")

                // Show error notification
                showNotification(
                    "Time Entry Failed",
                    "Failed to record time entry. Status: ${response.statusCode()}",
                    NotificationType.ERROR
                )

                return false
            }

        } catch (e: Exception) {
            logger.error("Error creating time entry", e)

            // Show error notification
            showNotification(
                "Time Entry Error",
                "Error recording time entry: ${e.message}",
                NotificationType.ERROR
            )

            return false
        }
    }

    private fun showNotification(title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Chrontic Time Tracker")
            .createNotification(title, content, type)
            .notify(null)
    }
}
