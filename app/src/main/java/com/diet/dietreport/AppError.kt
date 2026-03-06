package com.diet.dietreport

sealed class AppError {
    data class AuthError(val message: String, val cause: Throwable? = null) : AppError()
    data class DatabaseError(val message: String, val cause: Throwable? = null) : AppError()
    data class SchedulerError(val message: String, val cause: Throwable? = null) : AppError()
    data class CameraError(val message: String, val cause: Throwable? = null) : AppError()
    data class NetworkError(val message: String, val cause: Throwable? = null) : AppError()
}
