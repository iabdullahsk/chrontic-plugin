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
    val userId: Long,
    val projectId: Long,
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

class ClocklyticService {
    private val logger = Logger.getInstance(ClocklyticService::class.java)
    private val settings = ClocklyticSettings.getInstance()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun createTimeEntry(jiraTicket: String?, branchName: String?, durationMinutes: Int): Boolean {
        if (settings.apiKey.isBlank()) {
            logger.warn("API key not configured. Please configure the plugin settings.")
            return false
        }

        try {
            val userId = settings.userId ?: throw IllegalArgumentException("User ID not configured. Please configure the plugin settings.")
            val projectId = settings.projectId ?: throw IllegalArgumentException("Project ID not configured. Please configure the plugin settings.")

            val startTime = LocalDateTime.now().minusMinutes(durationMinutes.toLong())
            val endTime = LocalDateTime.now()

            // Create description based on available information
            val description = when {
                !jiraTicket.isNullOrBlank() -> jiraTicket
                !branchName.isNullOrBlank() -> branchName
                else -> "Auto-tracked via IntelliJ plugin: branch name could not be determined"
            }

            val timeEntry = TimeEntryRequest(
                userId = userId,
                projectId = projectId,
                startTime = startTime,
                endTime = endTime,
                hoursWorked = durationMinutes.toDouble() / 60.0,
                description = description
            )

            val requestBody = json.encodeToString(timeEntry)
            logger.info("Creating time entry with description: $description")

            val request = HttpRequest.newBuilder()
                .uri(URI.create("${settings.apiBaseUrl}/api/time-entries"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer ${settings.apiKey}")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                logger.info("Time entry created successfully. Duration: $durationMinutes minutes, Description: $description")
                return true
            } else {
                logger.error("Failed to create time entry. Status: ${response.statusCode()}, Body: ${response.body()}")
                return false
            }

        } catch (e: Exception) {
            logger.error("Error creating time entry", e)
            return false
        }
    }
}
