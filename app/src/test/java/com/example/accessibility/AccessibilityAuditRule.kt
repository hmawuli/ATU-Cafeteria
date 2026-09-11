package com.example.accessibility

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Custom Compose test rule to audit existing UI components for missing content descriptions,
 * ensuring all cafeteria interaction elements are fully navigable by TalkBack users.
 */
class AccessibilityAuditRule(
    private val composeTestRule: ComposeContentTestRule
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                base.evaluate()
            }
        }
    }

    fun auditContentDescriptions(): AuditResult {
        val nodes = composeTestRule.onAllNodes(
            SemanticsMatcher("Interactive or Image Semantics") { node ->
                val config = node.config
                val isClickable = config.contains(SemanticsActions.OnClick)
                val hasContentDescription = config.contains(SemanticsProperties.ContentDescription)
                val hasText = config.contains(SemanticsProperties.Text)
                isClickable || hasContentDescription || hasText
            }
        )

        var totalChecked = 0
        var passed = 0
        var unlabelledCount = 0
        val violations = mutableListOf<String>()

        val semanticsNodes = nodes.fetchSemanticsNodes()
        for (node in semanticsNodes) {
            totalChecked++
            val config = node.config
            val isClickable = config.contains(SemanticsActions.OnClick)
            val contentDesc = config.getOrNull(SemanticsProperties.ContentDescription)
            val textList = config.getOrNull(SemanticsProperties.Text)

            val hasLabel = (contentDesc != null && contentDesc.any { it.isNotBlank() }) ||
                    (textList != null && textList.any { it.text.isNotBlank() })

            if (isClickable && !hasLabel) {
                unlabelledCount++
                violations.add("Clickable node ID ${node.id} is missing a TalkBack content description.")
            } else {
                passed++
            }
        }

        return AuditResult(
            totalChecked = totalChecked,
            passed = passed,
            unlabelledCount = unlabelledCount,
            violations = violations
        )
    }

    data class AuditResult(
        val totalChecked: Int,
        val passed: Int,
        val unlabelledCount: Int,
        val violations: List<String>
    ) {
        val isCompliant: Boolean get() = unlabelledCount == 0
    }
}
