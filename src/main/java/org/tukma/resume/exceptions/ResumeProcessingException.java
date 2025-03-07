package org.tukma.resume.exceptions;

/**
 * Exception thrown when there are issues processing resume data
 */
public class ResumeProcessingException extends RuntimeException {
    
    public ResumeProcessingException(String message) {
        super(message);
    }
    
    public ResumeProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
