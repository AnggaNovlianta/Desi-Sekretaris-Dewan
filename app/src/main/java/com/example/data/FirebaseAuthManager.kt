package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"
    private var auth: FirebaseAuth? = null
    private var isInitialized = false

    fun getAuth(context: Context): FirebaseAuth? {
        if (!isInitialized) {
            try {
                // Ensure Firebase is initialized
                FirestoreSyncManager.initialize(context) { }
                auth = FirebaseAuth.getInstance()
                isInitialized = true
                Log.d(TAG, "FirebaseAuth berhasil diinisialisasi.")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menginisialisasi FirebaseAuth", e)
                auth = null
            }
        }
        return auth
    }

    // Register User with Email and Password, and save their role in Firestore
    suspend fun registerUser(
        context: Context,
        email: String,
        password: String,
        role: String, // "SEKWAN" or "ANGGOTA"
        onLog: (String) -> Unit
    ): Result<String> {
        val firebaseAuth = getAuth(context) ?: return Result.failure(Exception("Platform Firebase belum terkonfigurasi. Menggunakan Simulasi Lokal."))

        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                val db = FirebaseFirestore.getInstance()
                val userData = mapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "role" to role,
                    "createdAt" to System.currentTimeMillis()
                )
                // Save role to Firestore users collection for Data Access Control
                db.collection("users")
                    .document(user.uid)
                    .set(userData)
                    .await()
                
                onLog("Auth: Pengguna baru berhasil terdaftar di Firebase dengan level akses [${role}].")
                Result.success(role)
            } else {
                Result.failure(Exception("Registrasi gagal: Gagal mengambil data pengguna."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registration", e)
            Result.failure(e)
        }
    }

    // Login user and fetch their assigned role from Firestore
    suspend fun loginUser(
        context: Context,
        email: String,
        password: String,
        onLog: (String) -> Unit
    ): Result<String> {
        val firebaseAuth = getAuth(context) ?: return Result.failure(Exception("Platform Firebase belum terkonfigurasi. Menggunakan Simulasi Lokal."))

        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                if (doc.exists()) {
                    val role = doc.getString("role") ?: "ANGGOTA"
                    onLog("Auth: Login berhasil sebagai [${role}]. Sesi aman terverifikasi via Firebase (SSL/JWT).")
                    Result.success(role)
                } else {
                    // Try to migrate or assign a default role based on email context
                    val roleStr = if (email.contains("sekwan", ignoreCase = true)) "SEKWAN" else "ANGGOTA"
                    val userData = mapOf(
                        "uid" to user.uid,
                        "email" to email,
                        "role" to roleStr,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("users").document(user.uid).set(userData).await()
                    onLog("Auth: Login berhasil. Profil level akses baru dialokasikan otomatis: [${roleStr}].")
                    Result.success(roleStr)
                }
            } else {
                Result.failure(Exception("Login gagal: Pengguna kosong."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in", e)
            Result.failure(e)
        }
    }

    // Get current logged-in user email
    fun getCurrentUserEmail(context: Context): String? {
        return getAuth(context)?.currentUser?.email
    }

    // Fetch role of current logged-in user from Firestore
    suspend fun getCurrentUserRole(context: Context): String? {
        val firebaseAuth = getAuth(context) ?: return null
        val user = firebaseAuth.currentUser ?: return null
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(user.uid).get().await()
            if (doc.exists()) {
                doc.getString("role")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mendapatkan peran pengguna", e)
            null
        }
    }

    // Sign out currently active session
    fun logout(context: Context, onLog: (String) -> Unit) {
        try {
            getAuth(context)?.signOut()
            onLog("Auth: Sesi aman Firebase telah diakhiri (Logged out).")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal Logout", e)
        }
    }
}
