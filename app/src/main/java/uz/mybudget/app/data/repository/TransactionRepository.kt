package uz.mybudget.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import uz.mybudget.app.data.model.Transaction

class TransactionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private fun collection() = db.collection("users")
        .document(auth.currentUser?.uid ?: error("Foydalanuvchi login qilmagan"))
        .collection("transactions")

    suspend fun add(transaction: Transaction) {
        val ref = collection().document()
        ref.set(transaction.copy(id = ref.id)).await()
    }

    suspend fun update(transaction: Transaction) {
        collection().document(transaction.id)
            .set(transaction.copy(updatedAt = System.currentTimeMillis()))
            .await()
    }

    suspend fun delete(id: String) {
        collection().document(id).delete().await()
    }

    suspend fun getLatest(limit: Long = 50): List<Transaction> {
        return collection()
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .toObjects(Transaction::class.java)
    }

    suspend fun getInRange(start: Long, end: Long): List<Transaction> {
        return collection()
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Transaction::class.java)
    }
}
