/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   GlobalExceptionHandler.java                        :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: marouan <marouan@student.42.fr>            +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/30 11:09:53 by kchaouki          #+#    #+#             */
/*   Updated: 2026/09/17 11:34:54 by marouan          ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.filmexa.stream.common.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.lang.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


import com.filmexa.stream.common.utils.ErrorResponse;
import com.filmexa.stream.common.exception.NotFoundException;
import com.filmexa.stream.common.exception.InvalidPaginationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler( NotFoundException.class )
    public ResponseEntity<ErrorResponse> handledNotFound( NotFoundException ex ) {
        return this.builderResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND
        );
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

   // Invalid JSON
   @ExceptionHandler( HttpMessageNotReadableException.class ) 
   public ResponseEntity<ErrorResponse> handleInvalidJson( HttpMessageNotReadableException ex ) {
        return this.builderResponse(
                "Invalid request body",
                HttpStatus.BAD_REQUEST
        );
   }
   
   @ExceptionHandler( InvalidPaginationException.class )
    public ResponseEntity<ErrorResponse> handleInvalidPagination(
            InvalidPaginationException ex) {

        return this.builderResponse( 
            ex.getMessage(),
            HttpStatus.BAD_REQUEST
        );
    }
    
    @ExceptionHandler( {
        MethodArgumentTypeMismatchException.class,
        ConstraintViolationException.class,
        // MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            Exception ex ) {
            
        return builderResponse(
            "Invalid request",
            HttpStatus.BAD_REQUEST
        );
    }

   @ExceptionHandler( HttpRequestMethodNotSupportedException.class )
    public ResponseEntity<ErrorResponse> handleMethodNotSupported( HttpRequestMethodNotSupportedException ex ) {
            
                return this.builderResponse( 
                    "HTTP method not allowed",
                    HttpStatus.METHOD_NOT_ALLOWED
                );
    }
    
    @ExceptionHandler( NoResourceFoundException.class )
    public ResponseEntity<ErrorResponse> handleMethodNotSupported( NoResourceFoundException ex ) {
        return this.builderResponse( 
            "Not Found",
            HttpStatus.NOT_FOUND
        );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException( Exception ex ) {
        System.out.println(ex.getMessage());
            return builderResponse( 
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
    }
    
    private ResponseEntity<ErrorResponse> builderResponse( 
        String message,
        HttpStatus status
    ) {
        ErrorResponse response = new ErrorResponse(
                status.value(),
                message
            );
    
            return ResponseEntity
                .status( status.value() )
                .body( response );
    }
}
