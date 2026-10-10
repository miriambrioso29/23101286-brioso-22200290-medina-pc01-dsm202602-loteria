package com.example.pc1.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseAuthManager {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun registerUser(name: String, email: String, password: String): Result<Unit>
    {
        return try {
            //Firebase auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Usuario inválido")

            //Firebase firestore
            val user = hashMapOf(
                "name" to name,
                "email" to email
            )
            firestore.collection("users").document(uid).set(user).await()

            Result.success(Unit)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<Unit>{
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun currentUid(): String? = auth.currentUser?.uid

    fun currentEmail(): String = auth.currentUser?.email ?: ""
}
