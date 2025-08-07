package clocklytic.clocklyticplugin

import org.junit.Test
import org.junit.Assert.*

class SimpleGitServiceTest {

    @Test
    fun testJiraTicketExtraction() {
        val testCases = mapOf(
            "feature/PROJ-123-new-feature" to "PROJ-123",
            "bugfix/ABC-456" to "ABC-456",
            "hotfix/TICKET-789-urgent-fix" to "TICKET-789",
            "main" to null,
            "feature/no-ticket" to null
        )

        val regex = "([A-Z]{2,10}-\\d+)"

        testCases.forEach { (branchName, expected) ->
            val result = extractJiraTicket(branchName, regex)
            assertEquals("Failed for branch: $branchName", expected, result)
        }
    }

    @Test
    fun testCustomJiraRegex() {
        val customRegex = "(CUSTOM-\\d{4})"

        val result = extractJiraTicket("feature/CUSTOM-1234-test", customRegex)
        assertEquals("CUSTOM-1234", result)

        val noMatch = extractJiraTicket("feature/PROJ-123-test", customRegex)
        assertNull("Should not match default pattern", noMatch)
    }

    @Test
    fun testBasicRegexValidation() {
        val regex = "([A-Z]{2,10}-\\d+)"

        // Test a simple case that should not match
        val result = extractJiraTicket("feature/no-ticket-here", regex)
        assertNull("Should be null for branch with no ticket pattern", result)

        // Test a case that should match
        val validResult = extractJiraTicket("feature/TEST-123", regex)
        assertEquals("TEST-123", validResult)
    }

    @Test
    fun testBranchNamesWithMissingKeyword() {
        val regex = "([A-Z]{2,10}-\\d+)"

        // Test branch names that contain "missing" but are valid branch names
        val branchesWithMissing = mapOf(
            "branch_name_missing" to null, // No JIRA ticket
            "feature/missing_data_fix" to null, // No JIRA ticket
            "feature/PROJ-123-missing-validation" to "PROJ-123", // Has JIRA ticket
            "bugfix/missing-ABC-456-feature" to "ABC-456" // Has JIRA ticket
        )

        branchesWithMissing.forEach { (branchName, expected) ->
            val result = extractJiraTicket(branchName, regex)
            assertEquals("Failed for branch with 'missing': $branchName", expected, result)
        }
    }

    @Test
    fun testGitCommandOutputFiltering() {
        // Test filtering of git command output that might contain command text
        val gitOutputScenarios = listOf(
            "git branch --show-current\nfeature/PROJ-123" to "PROJ-123",
            "main" to null,
            "develop" to null,
            "feature/ABC-456-test" to "ABC-456"
        )

        gitOutputScenarios.forEach { (rawOutput, expectedTicket) ->
            // Simulate cleaning git output like the real GitService
            val cleanBranch = cleanGitOutput(rawOutput)
            val result = if (cleanBranch != null) extractJiraTicket(cleanBranch, "([A-Z]{2,10}-\\d+)") else null
            assertEquals("Failed for git output: $rawOutput", expectedTicket, result)
        }
    }

    @Test
    fun testBranchNameAsDescription() {
        // Test various branch names that would be used as descriptions
        val branchNames = listOf(
            "feature/add-user-authentication",
            "bugfix/fix-login-issue",
            "hotfix/urgent-security-patch",
            "develop",
            "main",
            "release/v1.2.0",
            "branch_name_missing"
        )

        branchNames.forEach { branchName ->
            // All these should be valid branch names for descriptions
            assertTrue("Branch name should be valid: $branchName",
                branchName.isNotBlank() && !branchName.startsWith("git "))
        }
    }

    @Test
    fun testJiraTicketCaseHandling() {
        val regex = "([A-Z]{2,10}-\\d+)"

        // Test case insensitive matching and uppercase conversion
        val caseTestCases = mapOf(
            "feature/proj-123-test" to "PROJ-123", // lowercase to uppercase
            "feature/Proj-456-test" to "PROJ-456", // mixed case to uppercase
            "feature/PROJ-789-test" to "PROJ-789"  // already uppercase
        )

        caseTestCases.forEach { (branchName, expected) ->
            val result = extractJiraTicket(branchName, regex)
            assertEquals("Case handling failed for: $branchName", expected, result)
        }
    }

    @Test
    fun testComplexBranchNames() {
        val regex = "([A-Z]{2,10}-\\d+)"

        // Test complex real-world branch naming scenarios
        val complexCases = mapOf(
            "feature/PROJ-123-implement-user-auth-with-oauth" to "PROJ-123",
            "bugfix/ABC-456_fix_null_pointer_exception" to "ABC-456",
            "hotfix/URGENT-789.critical.security.fix" to "URGENT-789",
            "release/v2.0.0-PROJ-999-final" to "PROJ-999",
            "feature/user-story-XYZ-123-backend" to "XYZ-123"
        )

        complexCases.forEach { (branchName, expected) ->
            val result = extractJiraTicket(branchName, regex)
            assertEquals("Complex branch test failed for: $branchName", expected, result)
        }
    }

    @Test
    fun testMultipleTicketsInBranch() {
        val regex = "([A-Z]{2,10}-\\d+)"

        // Test branches with multiple potential tickets (should get first one)
        val multiTicketCases = mapOf(
            "feature/PROJ-123-relates-to-ABC-456" to "PROJ-123", // Should get first match
            "merge/ABC-111-and-DEF-222-combined" to "ABC-111"    // Should get first match
        )

        multiTicketCases.forEach { (branchName, expected) ->
            val result = extractJiraTicket(branchName, regex)
            assertEquals("Multi-ticket test failed for: $branchName", expected, result)
        }
    }

    private fun extractJiraTicket(branchName: String, regex: String): String? {
        val pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(branchName)

        return if (matcher.find()) {
            matcher.group(1).uppercase()
        } else null
    }

    // Helper method to simulate git output cleaning
    private fun cleanGitOutput(rawOutput: String): String? {
        return rawOutput.trim()
            .lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotEmpty() && !line.startsWith("git ")
            }
            .filter { line ->
                !line.lowercase().startsWith("error:") &&
                !line.lowercase().startsWith("fatal:") &&
                !line.lowercase().startsWith("warning:")
            }
            .firstOrNull()
    }
}
