package com.jevzo.auth.exception

class UserEmailNotFoundException(email: String) : RuntimeException("User with email $email not found")
class UserIdNotFoundException(id: Long?) : RuntimeException("User with id $id not found")
class EmailExistsException(email: String) : RuntimeException("A user with the email $email already exists")
class FailedToCreateUserException(message: String) : RuntimeException(message)