package com.diet.dietreport

import android.content.Context
import com.diet.dietreport.auth.AuthService
import com.diet.dietreport.auth.data.User

class FakeAuthService(private val user: User) : AuthService {
    override suspend fun signIn(context: Context): User = user
}
