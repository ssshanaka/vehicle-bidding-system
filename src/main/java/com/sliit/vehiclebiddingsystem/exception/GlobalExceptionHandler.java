package com.sliit.vehiclebiddingsystem.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.exceptions.TemplateProcessingException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Thymeleaf template processing exceptions
     */
    @ExceptionHandler({TemplateProcessingException.class, TemplateInputException.class, TemplateEngineException.class})
    public ModelAndView handleThymeleafException(Exception ex, HttpServletRequest request) {
        logger.error("Thymeleaf template processing error for request: {} - {}", 
                    request.getRequestURI(), ex.getMessage(), ex);
        
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("errorMessage", "There was an issue rendering the page. Please try again.");
        modelAndView.addObject("errorDetails", "Template processing error");
        modelAndView.addObject("requestUri", request.getRequestURI());
        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        
        return modelAndView;
    }

    /**
     * Handles general runtime exceptions that might occur during template rendering
     */
    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        logger.error("Runtime exception during request processing: {} - {}", 
                    request.getRequestURI(), ex.getMessage(), ex);
        
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("errorMessage", "An unexpected error occurred. Please try again.");
        modelAndView.addObject("errorDetails", "Runtime exception");
        modelAndView.addObject("requestUri", request.getRequestURI());
        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        
        return modelAndView;
    }

    /**
     * Handles null pointer exceptions that might occur during template rendering
     */
    @ExceptionHandler(NullPointerException.class)
    public ModelAndView handleNullPointerException(NullPointerException ex, HttpServletRequest request) {
        logger.error("Null pointer exception during request processing: {} - {}", 
                    request.getRequestURI(), ex.getMessage(), ex);
        
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("errorMessage", "A data processing error occurred. Please try again.");
        modelAndView.addObject("errorDetails", "Data processing error");
        modelAndView.addObject("requestUri", request.getRequestURI());
        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        
        return modelAndView;
    }

    /**
     * Handles illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        logger.error("Illegal argument exception for request: {} - {}", 
                    request.getRequestURI(), ex.getMessage(), ex);
        
        return ResponseEntity.badRequest()
                .body("Invalid request parameters: " + ex.getMessage());
    }

    /**
     * Handles JWT-related exceptions
     */
    @ExceptionHandler({ExpiredJwtException.class, MalformedJwtException.class, 
                      UnsupportedJwtException.class, SignatureException.class})
    public ResponseEntity<String> handleJwtException(Exception ex, HttpServletRequest request) {
        logger.warn("JWT exception for request: {} - {}", 
                   request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Authentication failed: Invalid or expired token");
    }

    /**
     * Handles authentication exceptions
     */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<String> handleAuthenticationException(Exception ex, HttpServletRequest request) {
        logger.warn("Authentication exception for request: {} - {}", 
                   request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Authentication failed: " + ex.getMessage());
    }

    /**
     * Handles all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Unexpected exception during request processing: {} - {}", 
                    request.getRequestURI(), ex.getMessage(), ex);
        
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("errorMessage", "An unexpected error occurred. Please try again later.");
        modelAndView.addObject("errorDetails", "Unexpected error");
        modelAndView.addObject("requestUri", request.getRequestURI());
        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        
        return modelAndView;
    }
}
