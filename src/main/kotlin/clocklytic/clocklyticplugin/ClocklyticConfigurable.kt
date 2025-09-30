package clocklytic.clocklyticplugin

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class ClocklyticConfigurable : Configurable {
    private val settings = ClocklyticSettings.getInstance()

    private val apiBaseUrlField = JBTextField(settings.apiBaseUrl, 30)
    private val apiKeyField = JBPasswordField()
    private val trackingIntervalSpinner = JSpinner(SpinnerNumberModel(settings.trackingIntervalMinutes, 1, 60, 1))
    private val enableAutoTrackingCheckbox = JBCheckBox("Enable automatic time tracking", settings.enableAutoTracking)
    private val jiraRegexField = JBTextField(settings.jiraTicketRegex, 30)

    private var panel: JPanel? = null

    override fun getDisplayName(): String = "Clocklytic Time Tracker"

    override fun createComponent(): JComponent {
        apiKeyField.text = settings.apiKey

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("API Base URL:", apiBaseUrlField)
            .addTooltip("The base URL of your Clocklytic API (e.g., http://localhost:8080)")
            .addLabeledComponent("API Key:", apiKeyField)
            .addTooltip("Your Clocklytic API authentication key")
            .addLabeledComponent("Tracking Interval (minutes):", trackingIntervalSpinner)
            .addTooltip("How often to send time entries (1-60 minutes)")
            .addComponent(enableAutoTrackingCheckbox)
            .addTooltip("Enable or disable automatic time tracking")
            .addLabeledComponent("JIRA Ticket Regex:", jiraRegexField)
            .addTooltip("Regular expression to extract JIRA ticket from branch name")
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        return apiBaseUrlField.text != settings.apiBaseUrl ||
                String(apiKeyField.password) != settings.apiKey ||
                trackingIntervalSpinner.value != settings.trackingIntervalMinutes ||
                enableAutoTrackingCheckbox.isSelected != settings.enableAutoTracking ||
                jiraRegexField.text != settings.jiraTicketRegex
    }

    override fun apply() {
        settings.apiBaseUrl = apiBaseUrlField.text.trim()
        settings.apiKey = String(apiKeyField.password)
        settings.trackingIntervalMinutes = trackingIntervalSpinner.value as Int
        settings.enableAutoTracking = enableAutoTrackingCheckbox.isSelected
        settings.jiraTicketRegex = jiraRegexField.text.trim()
    }

    override fun reset() {
        apiBaseUrlField.text = settings.apiBaseUrl
        apiKeyField.text = settings.apiKey
        trackingIntervalSpinner.value = settings.trackingIntervalMinutes
        enableAutoTrackingCheckbox.isSelected = settings.enableAutoTracking
        jiraRegexField.text = settings.jiraTicketRegex
    }
}
