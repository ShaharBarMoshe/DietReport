package com.diet.dietreport.auth

import android.content.Context
import com.diet.dietreport.auth.data.User

interface AuthService {
    suspend fun signIn(context: Context): User
}
