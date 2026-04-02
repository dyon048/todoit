package com.example.todoit.data.remote.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.sheets.v4.SheetsScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val SCOPES = listOf(
            SheetsScopes.SPREADSHEETS,
            "https://www.googleapis.com/auth/drive.file",
        )
    }

    private val signInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope(SheetsScopes.SPREADSHEETS),
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"),
            )
            .build()

    /**
     * Parses the [Intent] returned by the Google sign-in Activity and returns the signed-in
     * [GoogleSignInAccount], or throws [ApiException] if sign-in failed.
     *
     * The underlying [GoogleSignIn] API is deprecated in favour of Credential Manager, but
     * migration requires a separate OAuth client type. The suppression is intentionally scoped
     * to this single method so ViewModels remain warning-free.
     */
    @Suppress("DEPRECATION")
    fun handleSignInResult(data: Intent): GoogleSignInAccount {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return task.getResult(ApiException::class.java)
    }

    fun getClient(): GoogleSignInClient =
        GoogleSignIn.getClient(context, signInOptions)

    fun getSignInIntent(): Intent = getClient().signInIntent

    fun getSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun isSignedIn(): Boolean = getSignedInAccount() != null

    suspend fun signOut() {
        suspendCancellableCoroutine<Unit> { cont ->
            getClient().signOut()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    /** Returns a credential object for making authenticated Sheets API calls. */
    fun getCredential(account: GoogleSignInAccount): GoogleAccountCredential =
        GoogleAccountCredential.usingOAuth2(context, SCOPES).also {
            it.selectedAccount = account.account
        }
}
