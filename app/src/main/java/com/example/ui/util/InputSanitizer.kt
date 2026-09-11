package com.example.ui.util

import java.util.regex.Pattern

/**
 * InputSanitizer utility object to sanitize user-provided text inputs in feedback,
 * reviews, and profile edit screens to prevent potential injection attacks before processing.
 */
object InputSanitizer {
    private val HTML_PATTERN = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE)
    private val SCRIPT_PATTERN = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE)
    private val DANGEROUS_SQL_KEYWORDS = Pattern.compile(
        "(?i)\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT|MERGE|SELECT|UPDATE|UNION|TRUNCATE)\\b"
    )

    /**
     * Sanitize general text by stripping HTML, removing script tags,
     * and escaping special characters.
     */
    fun sanitizeText(input: String?): String {
        if (input.isNullOrBlank()) return ""

        var clean = input.trim()
        // Strip <script> tags
        clean = SCRIPT_PATTERN.matcher(clean).replaceAll("")
        // Strip general HTML tags
        clean = HTML_PATTERN.matcher(clean).replaceAll("")
        // Neutralize dangerous SQL keywords if injected standalone
        clean = DANGEROUS_SQL_KEYWORDS.matcher(clean).replaceAll("[BLOCKED_KEYWORD]")
        // Escape special injection characters
        clean = clean
            .replace("'", "''")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        return clean.trim()
    }

    /**
     * Sanitize user full names / display names.
     */
    fun sanitizeName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val clean = sanitizeText(name)
        return clean.take(100)
    }

    /**
     * Sanitize feedback, reviews, and support message bodies.
     */
    fun sanitizeFeedback(feedback: String?): String {
        if (feedback.isNullOrBlank()) return ""
        val clean = sanitizeText(feedback)
        return clean.take(1000)
    }

    /**
     * Sanitize student ID / Index numbers (alphanumeric and dashes only).
     */
    fun sanitizeStudentId(id: String?): String {
        if (id.isNullOrBlank()) return ""
        return id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(30)
    }

    /**
     * Sanitize phone numbers.
     */
    fun sanitizePhone(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        return phone.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }.take(20)
    }
}
