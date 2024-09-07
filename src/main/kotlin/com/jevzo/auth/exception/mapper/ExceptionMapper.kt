package com.jevzo.auth.exception.mapper

import com.jevzo.auth.exception.EmailExistsException
import com.jevzo.auth.exception.FailedToCreateUserException
import com.jevzo.auth.exception.UserEmailNotFoundException
import com.jevzo.auth.exception.UserIdNotFoundException
import com.jevzo.auth.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionMapper {

    @ExceptionHandler(UserEmailNotFoundException::class)
    fun handleUserNotFoundException(exception: UserEmailNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(exception.message!!))
    }

    @ExceptionHandler(UserIdNotFoundException::class)
    fun handleUserNotFoundException(exception: UserIdNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(exception.message!!))
    }

    @ExceptionHandler(FailedToCreateUserException::class)
    fun handleFailedToCreateUserException(exception: FailedToCreateUserException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse(exception.message!!))
    }

    @ExceptionHandler(EmailExistsException::class)
    fun handleEmailExistsException(exception: EmailExistsException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(exception.message!!))
    }
}