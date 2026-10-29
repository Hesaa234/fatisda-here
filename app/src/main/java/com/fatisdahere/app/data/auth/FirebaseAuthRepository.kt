package com.fatisdahere.app.data.auth

import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.AuthRepository
import com.fatisdahere.app.data.firestore.toAppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/**
 * Login asli dengan Firebase Authentication.
 * Setelah email dan kata sandi benar, data user (nama, role, NIM/NIP) dibaca dari users/{uid}.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<AppUser> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email dan kata sandi wajib diisi."))
        }
        return try {
            val hasil = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val uid = hasil.user?.uid ?: return Result.failure(IllegalStateException("Gagal masuk."))
            val doc = db.collection("users").document(uid).get().await()
            val user = doc.toAppUser()
            if (user == null) {
                auth.signOut()
                Result.failure(
                    IllegalStateException("Akun belum terdaftar di data users (atau role-nya kosong). Hubungi admin.")
                )
            } else {
                Result.success(user)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            Result.failure(IllegalStateException("Tidak ada koneksi internet."))
        } catch (e: FirebaseAuthException) {
            Result.failure(IllegalArgumentException("Email atau kata sandi salah."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "Gagal masuk."))
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
