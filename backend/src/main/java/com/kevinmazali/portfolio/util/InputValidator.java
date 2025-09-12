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
    private static final int MAX_CHAT_ID_LENGTH = 50;
    private static final int MAX_REQUEST_ID_LENGTH = 100;
    
    /**
     * Validates and sanitizes a chat ID.
     * 
     * @param chatId the chat ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidChatId(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return false;
        }
        
        if (chatId.length() > MAX_CHAT_ID_LENGTH) {
            return false;
        }
        
        // Allow UUIDs or simple alphanumeric IDs
        return UUID_PATTERN.matcher(chatId).matches() || 
               SAFE_STRING_PATTERN.matcher(chatId).matches();
    }
    
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
    
    /**
     * Validates a path parameter (for conversation ID).
     * 
     * @param pathParam the path parameter to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPathParameter(String pathParam) {
        if (pathParam == null || pathParam.isBlank()) {
            return false;
        }
        
        try {
            long id = Long.parseLong(pathParam);
            return id > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
