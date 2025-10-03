# Chrontic Time Tracker Plugin

A JetBrains IntelliJ IDEA plugin that automatically tracks time spent on JIRA tickets by detecting ticket names from Git branch names and integrates with the Chrontic time tracking service.

## Features

- **Automatic Time Tracking**: Tracks time spent on projects at configurable intervals (default: 15 minutes)
- **JIRA Integration**: Automatically detects JIRA ticket IDs from Git branch names using configurable regex
- **Activity Detection**: Only tracks time when the IDE is actively being used
- **Branch Name Fallback**: Uses branch name (prefixed with project name) when no JIRA ticket is detected
- **Configurable Settings**: Customize API endpoint, tracking intervals, and JIRA ticket patterns

## Building the Plugin

### Prerequisites

- JDK 17 or higher
- Git (for cloning the repository)

### Build Instructions

1. **Clone the repository** (if not already cloned):
   ```bash
   git clone <repository-url>
   cd chrontic-plugin
   ```

2. **Build the plugin distribution**:
   ```bash
   ./gradlew buildPlugin
   ```

   On Windows:
   ```bash
   gradlew.bat buildPlugin
   ```

3. **Locate the plugin ZIP file**:

   After a successful build, the plugin distribution ZIP file will be located at:
   ```
   build/distributions/chrontic-plugin-1.0-SNAPSHOT.zip
   ```

## Installing the Plugin

### Option 1: Install from Disk (Recommended for Development)

1. Open your JetBrains IDE (IntelliJ IDEA, PhpStorm, PyCharm, etc.)
2. Go to **Settings/Preferences** → **Plugins**
3. Click the gear icon (⚙️) and select **Install Plugin from Disk...**
4. Navigate to `build/distributions/` and select `chrontic-plugin-1.0-SNAPSHOT.zip`
5. Click **OK** and restart the IDE when prompted

### Option 2: Test in Sandbox IDE

To test the plugin in a sandboxed IDE environment without installing it in your main IDE:

```bash
./gradlew runIde
```

This will launch a new IntelliJ IDEA instance with the plugin pre-installed.

## Configuration

After installing the plugin:

1. Go to **Settings/Preferences** → **Chrontic Time Tracker**
2. Configure the following settings:
   - **API Base URL**: The base URL of your Chrontic API (e.g., `http://localhost:8086`)
   - **API Key**: Your Chrontic API authentication key
   - **Tracking Interval**: How often to send time entries (1-60 minutes, default: 15)
   - **Enable automatic time tracking**: Toggle to enable/disable the plugin
   - **JIRA Ticket Regex**: Pattern to extract JIRA ticket from branch name (default: `([A-Z]{2,10}-\d+)`)

## How It Works

1. The plugin initializes when you open a project (if auto-tracking is enabled)
2. A scheduled task runs at your configured interval (default: every 15 minutes)
3. The activity detector checks if you're actively working in the IDE
4. If active, the plugin retrieves your current Git branch and extracts the JIRA ticket ID
5. Time entries are sent to the Chrontic API with:
   - **JIRA ticket found**: Uses the ticket ID as description (e.g., `PROJ-123`)
   - **No JIRA ticket**: Uses project name + branch name (e.g., `MyProject_feature/add-authentication`)
6. Each time entry includes start time, end time, hours worked, and description

## Development

### Running Tests

```bash
./gradlew test
```

### Building and Testing

```bash
./gradlew build
```

### Code Structure

- `src/main/kotlin/chrontic/chronticplugin/` - Main plugin source code
  - `ChronticPlugin.kt` - Entry point and initialization
  - `TimeTracker.kt` - Time tracking orchestration
  - `ActivityDetector.kt` - IDE activity monitoring
  - `GitService.kt` - Git branch and JIRA ticket extraction
  - `ChronticService.kt` - API client for Chrontic
  - `ChronticSettings.kt` - Plugin settings persistence
  - `ChronticConfigurable.kt` - Settings UI

- `src/test/kotlin/chrontic/chronticplugin/` - Test files

## Requirements

- **JetBrains IDE**: Compatible with IntelliJ IDEA builds 241 to 243.*
- **Git**: Project must be in a Git repository
- **Chrontic API**: Running Chrontic backend service

## Troubleshooting

### Plugin not tracking time

1. Check that **Enable automatic time tracking** is enabled in settings
2. Verify your **API Key** is correctly configured
3. Ensure your **API Base URL** points to your Chrontic backend
4. Check the IDE logs for error messages: **Help** → **Show Log in Finder/Explorer**

### JIRA ticket not detected

1. Verify your branch name matches the JIRA ticket regex pattern
2. Default pattern matches formats like: `PROJ-123`, `ABC-456`
3. Customize the regex pattern in settings if needed

### Time entries not appearing in Chrontic

1. Verify the Chrontic API is running and accessible
2. Check that the API endpoint URL is correct
3. Ensure the API key is valid
4. Review IDE logs for API errors

## License

[Add your license information here]

## Support

For issues and questions, please refer to the project's issue tracker.
