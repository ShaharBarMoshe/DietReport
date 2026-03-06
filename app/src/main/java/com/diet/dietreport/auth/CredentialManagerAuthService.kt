package com.diet.dietreport.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.diet.dietreport.auth.data.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class CredentialManagerAuthService : AuthService {

    companion object {
        // TODO: Replace with your actual Web Client ID from Firebase / Google Cloud Console
        private const val SERVER_CLIENT_ID = "YOUR_WEB_CLIENT_ID"
    }

    override suspend fun signIn(context: Context): User {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(SERVER_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        check(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Unexpected credential type: ${credential.type}" }
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
        return User(
            userId = googleIdToken.id,
            email = googleIdToken.id,
            displayName = googleIdToken.displayName ?: googleIdToken.id,
        )
    }
}
