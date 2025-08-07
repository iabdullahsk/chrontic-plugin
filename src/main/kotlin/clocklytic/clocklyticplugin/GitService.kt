package clocklytic.clocklyticplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.util.Key
import java.util.regex.Pattern
import java.io.File

class GitService(private val project: Project) {
    private val logger = Logger.getInstance(GitService::class.java)
    private val settings = ClocklyticSettings.getInstance()

    // Use configurable JIRA ticket pattern
    private val jiraPattern: Pattern
        get() = Pattern.compile(settings.jiraTicketRegex, Pattern.CASE_INSENSITIVE)

    fun getCurrentJiraTicket(): String? {
        try {
            val branchName = getCurrentGitBranch()
            if (branchName.isNullOrBlank()) {
                logger.warn("No current branch found")
                return null
            }

            logger.info("Current branch: $branchName")
            return extractJiraTicketFromBranch(branchName)

        } catch (e: Exception) {
            logger.error("Error getting git branch information", e)
            return null
        }
    }

    fun getCurrentBranchName(): String? {
        return getCurrentGitBranch()
    }

    private fun getCurrentGitBranch(): String? {
        try {
            // Get the project's base path
            val projectPath = project.basePath ?: return null

            // Find git repository root
            val gitDir = findGitDirectory(File(projectPath)) ?: return null

            // Execute git command to get current branch
            val commandLine = GeneralCommandLine("git", "branch", "--show-current")
            commandLine.workDirectory = gitDir.parentFile

            val processHandler = OSProcessHandler(commandLine)
            var result = ""
            var errorOutput = ""

            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    if (outputType.toString().contains("STDERR")) {
                        errorOutput += event.text
                    } else {
                        result += event.text
                    }
                }
            })

            processHandler.startNotify()
            processHandler.waitFor(5000) // 5 second timeout

            // Clean the result - remove command text, error messages, and whitespace
            val cleanResult = result.trim()
                .lines()
                .map { it.trim() }
                .filter { line ->
                    // Filter out empty lines and command text
                    line.isNotEmpty() && !line.startsWith("git ")
                }
                .filter { line ->
                    // Filter out error messages but allow branch names that might contain keywords
                    !line.lowercase().startsWith("error:") &&
                    !line.lowercase().startsWith("fatal:") &&
                    !line.lowercase().startsWith("warning:")
                }
                .firstOrNull()

            if (processHandler.exitCode == 0 && !cleanResult.isNullOrEmpty()) {
                logger.info("Detected git branch: $cleanResult")
                return cleanResult
            } else {
                logger.warn("Git command failed or returned invalid output. Exit code: ${processHandler.exitCode}, Output: '$result', Error: '$errorOutput'")
                return null
            }

        } catch (e: Exception) {
            logger.error("Error executing git command", e)
            return null
        }
    }

    private fun findGitDirectory(dir: File): File? {
        var currentDir: File? = dir
        while (currentDir != null) {
            val gitDir = File(currentDir, ".git")
            if (gitDir.exists()) {
                return gitDir
            }
            currentDir = currentDir.parentFile
        }
        return null
    }

    private fun extractJiraTicketFromBranch(branchName: String): String? {
        val matcher = jiraPattern.matcher(branchName)

        if (matcher.find()) {
            val ticket = matcher.group(1).uppercase()
            logger.info("Extracted JIRA ticket: $ticket from branch: $branchName")
            return ticket
        }

        logger.info("No JIRA ticket found in branch name: $branchName")
        return null
    }
}
