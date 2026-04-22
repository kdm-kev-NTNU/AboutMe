package com.kevinmazali.portfolio.util;

import java.util.regex.Pattern;

/**
 * Shared rules for {@link com.kevinmazali.portfolio.controller.QuestionController}: max length, XSS-ish
 * substring blocklist, and a Unicode-safe character class so prompts stay printable text without control chars.
 */
public class InputValidator {
    
    /** Letters, numbers, punctuation, and spaces only (no raw control characters). */
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}]*$");
    
    private static final int MAX_QUESTION_LENGTH = 3000;
    private static final int MAX_FEEDBACK_LENGTH = 4000;
    private static final int MAX_REQUEST_ID_LENGTH = 100;

    /**
     * Validates a question input.
     * 
     * @param question the question to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidQuestion(String question) {
        return isValidQuestion(question, MAX_QUESTION_LENGTH);
    }

    /**
     * @param maxLength maximum characters allowed (from {@code portfolio.ai.limits.max-question-chars})
     */
    public static boolean isValidQuestion(String question, int maxLength) {
        if (question == null || question.isBlank()) {
            return false;
        }
        
        if (question.length() > maxLength) {
            return false;
        }
        
        // Check for potentially malicious content
        String lowerQuestion = question.toLowerCase();
        if (lowerQuestion.contains("<script") || 
            lowerQuestion.contains("javascript:") ||
            lowerQuestion.contains("data:text/html") ||
            lowerQuestion.contains("vbscript:")) {
            return false;
        }
        
        return SAFE_STRING_PATTERN.matcher(question).matches();
    }
    
    /**
     * Validates a requester ID.
     * 
     * @param requesterId the requester ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidRequesterId(String requesterId) {
        if (requesterId == null || requesterId.isBlank()) {
            return false;
        }
        
        if (requesterId.length() > MAX_REQUEST_ID_LENGTH) {
            return false;
        }
        
        return SAFE_STRING_PATTERN.matcher(requesterId).matches();
    }
    
    /**
     * Validates a feedback message (same safety rules as questions, different max length).
     */
    public static boolean isValidFeedbackMessage(String message) {
        return isValidFeedbackMessage(message, MAX_FEEDBACK_LENGTH);
    }

    public static boolean isValidFeedbackMessage(String message, int maxLength) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (message.length() > maxLength) {
            return false;
        }
        String lower = message.toLowerCase();
        if (lower.contains("<script") ||
            lower.contains("javascript:") ||
            lower.contains("data:text/html") ||
            lower.contains("vbscript:")) {
            return false;
        }
        return SAFE_STRING_PATTERN.matcher(message).matches();
    }

    /**
     * Strips control characters (except common whitespace) and collapses runs of whitespace before persistence.
     * 
     * @param input the input to sanitize
     * @return sanitized string
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        // Replace other control characters with a space, then normalize whitespace (keeps words separated).
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
}
