package clocklytic.clocklyticplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import git4idea.GitUtil
import git4idea.repo.GitRepositoryManager
import com.intellij.openapi.diagnostic.Logger
import java.util.regex.Pattern

class GitService(private val project: Project) {
    private val logger = Logger.getInstance(GitService::class.java)

    // Common JIRA ticket patterns (e.g., PROJ-123, ABC-456)
    private val jiraPattern = Pattern.compile("([A-Z]{2,10}-\\d+)", Pattern.CASE_INSENSITIVE)

    fun getCurrentJiraTicket(): String? {
        try {
            val repositoryManager = GitRepositoryManager.getInstance(project)
            val repositories = repositoryManager.repositories

            if (repositories.isEmpty()) {
                logger.warn("No Git repositories found in project")
                return null
            }

            val repository = repositories.first()
            val currentBranch = repository.currentBranch

            if (currentBranch == null) {
                logger.warn("No current branch found")
                return null
            }

            val branchName = currentBranch.name
            logger.info("Current branch: $branchName")

            return extractJiraTicketFromBranch(branchName)

        } catch (e: VcsException) {
            logger.error("Error getting git branch information", e)
            return null
        }
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
