
package com.zora.admin

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

object AdminAuth {

    const val ADMIN_EMAIL = "zora24jb@gmail.com"

    private const val WEB_CLIENT_ID =
        "1006039613910-a6jurm431t9goln7a94ul35u937pii64.apps.googleusercontent.com"

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private fun getGoogleClient(activity: Activity) =
        GoogleSignIn.getClient(
            activity,
            GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build()
        )

    fun signInIntent(activity: Activity): Intent {
        return getGoogleClient(activity).signInIntent
    }

    fun signIn(
        activity: Activity,
        data: Intent?,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            val account = task.getResult(ApiException::class.java)

            val idToken = account.idToken

            if (idToken.isNullOrBlank()) {
                onResult(
                    false,
                    "ID Token kosong. Periksa Web Client ID Firebase."
                )
                return
            }

            val credential = GoogleAuthProvider.getCredential(
                idToken,
                null
            )

            auth.signInWithCredential(credential)
                .addOnCompleteListener { result ->

                    if (!result.isSuccessful) {
                        onResult(
                            false,
                            "Firebase Auth gagal: ${
                                result.exception?.message ?: "Unknown error"
                            }"
                        )
                        return@addOnCompleteListener
                    }

                    val user = auth.currentUser
                    val email = user?.email.orEmpty()
                    val verified = user?.isEmailVerified == true

                    if (
                        email.equals(
                            ADMIN_EMAIL,
                            ignoreCase = true
                        ) && verified
                    ) {
                        onResult(
                            true,
                            "Login admin berhasil"
                        )
                    } else {
                        auth.signOut()

                        getGoogleClient(activity).signOut()

                        onResult(
                            false,
                            "Akun tidak memiliki akses administrator."
                        )
                    }
                }

        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                10 -> "DEVELOPER_ERROR (10): Periksa SHA-1, package name, dan OAuth Client ID."
                12500 -> "SIGN_IN_FAILED (12500): Periksa konfigurasi Google Sign-In."
                12501 -> "Login dibatalkan oleh pengguna."
                7 -> "Tidak ada koneksi internet."
                else -> "Google Sign-In gagal. Kode: ${e.statusCode}"
            }

            onResult(false, errorMessage)

        } catch (e: Exception) {
            onResult(
                false,
                "Login gagal: ${e.message ?: "Kesalahan tidak diketahui"}"
            )
        }
    }

    fun isAdmin(): Boolean {
        val user = auth.currentUser

        return user?.email.equals(
            ADMIN_EMAIL,
            ignoreCase = true
        ) && user?.isEmailVerified == true
    }

    fun signOut() {
        auth.signOut()
    }
}
