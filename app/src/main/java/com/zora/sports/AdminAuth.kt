package com.zora.sports

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

object AdminAuth {

    const val ADMIN_EMAIL = "zora24jb@gmail.com"

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    fun signInIntent(activity: Activity): Intent {
        val options = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(
                activity.getString(R.string.default_web_client_id)
            )
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(activity, options).signInIntent
    }

    fun signIn(
        activity: Activity,
        data: Intent?,
        onResult: (Boolean, String) -> Unit
    ) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            val account = task.getResult(
                com.google.android.gms.common.api.ApiException::class.java
            )

            val credential = GoogleAuthProvider.getCredential(
                account.idToken,
                null
            )

            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user
                    val email = user?.email.orEmpty()
                    val verified = user?.isEmailVerified == true

                    if (
                        email.equals(ADMIN_EMAIL, ignoreCase = true) &&
                        verified
                    ) {
                        onResult(true, "Login admin berhasil")
                    } else {
                        auth.signOut()
                        onResult(false, "Akun ini bukan admin ZORASPORTS")
                    }
                }
                .addOnFailureListener {
                    onResult(
                        false,
                        it.message ?: "Autentikasi Firebase gagal"
                    )
                }

        } catch (e: Exception) {
            onResult(
                false,
                e.message ?: "Login Google dibatalkan atau gagal"
            )
        }
    }

    fun isAdmin(): Boolean {
        val user = auth.currentUser
        return user?.email.equals(ADMIN_EMAIL, ignoreCase = true) &&
            user?.isEmailVerified == true
    }

    fun signOut() {
        auth.signOut()
    }
}
