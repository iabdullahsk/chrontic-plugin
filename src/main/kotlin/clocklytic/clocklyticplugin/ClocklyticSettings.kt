package clocklytic.clocklyticplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "ClocklyticSettings",
    storages = [Storage("clocklytic-plugin.xml")]
)
@Service
class ClocklyticSettings : PersistentStateComponent<ClocklyticSettings> {

    var apiBaseUrl: String = "http://localhost:8086"
    var apiKey: String = ""
    var userId: Long? = null
    var projectId: Long? = null
    var trackingIntervalMinutes: Int = 15
    var activityTimeoutMinutes: Int = 5
    var enableAutoTracking: Boolean = true
    var jiraTicketRegex: String = "([A-Z]{2,10}-\\d+)"

    override fun getState(): ClocklyticSettings = this

    override fun loadState(state: ClocklyticSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): ClocklyticSettings {
            return ApplicationManager.getApplication().getService(ClocklyticSettings::class.java)
        }
    }
}
