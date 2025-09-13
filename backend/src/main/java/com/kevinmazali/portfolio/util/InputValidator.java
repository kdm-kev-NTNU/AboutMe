package com.kevinmazali.portfolio.util;

import java.util.regex.Pattern;

/**
 * Utility class for input validation and sanitization.
 */
public class InputValidator {
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}]*$");
    
    private static final int MAX_QUESTION_LENGTH = 3000;
    private static final int MAX_REQUEST_ID_LENGTH = 100;
    
    // chatId validation removed
    
    /**
     * Validates a question input.
     * 
     * @param question the question to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        
        if (question.length() > MAX_QUESTION_LENGTH) {
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
     * Sanitizes a string by removing potentially dangerous characters.
     * 
     * @param input the input to sanitize
     * @return sanitized string
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove control characters and normalize whitespace
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    // conversation path parameter validation removed
}
