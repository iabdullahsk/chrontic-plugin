package chrontic.chronticplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "ChronticSettings",
    storages = [Storage("chrontic-plugin.xml")]
)
@Service
class ChronticSettings : PersistentStateComponent<ChronticSettings> {

    var apiBaseUrl: String = "http://localhost:8086"
    var apiKey: String = ""
    var trackingIntervalMinutes: Int = 15
    var activityTimeoutMinutes: Int = 5
    var enableAutoTracking: Boolean = true
    var jiraTicketRegex: String = "([A-Z]{2,10}-\\d+)"

    override fun getState(): ChronticSettings = this

    override fun loadState(state: ChronticSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): ChronticSettings {
            return ApplicationManager.getApplication().getService(ChronticSettings::class.java)
        }
    }
}
