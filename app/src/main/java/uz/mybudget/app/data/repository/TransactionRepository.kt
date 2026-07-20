package uz.mybudget.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.mybudget.app.data.model.Transaction
import java.util.UUID

class TransactionRepository(context: Context) {
    private val database = BudgetDatabase.getInstance(context.applicationContext)

    suspend fun add(transaction: Transaction) = withContext(Dispatchers.IO) {
        val item = transaction.copy(id = transaction.id.ifBlank { UUID.randomUUID().toString() })
        database.writableDatabase.insertOrThrow(TABLE_TRANSACTIONS, null, item.toValues())
    }

    suspend fun update(transaction: Transaction) = withContext(Dispatchers.IO) {
        val updated = transaction.copy(updatedAt = System.currentTimeMillis())
        database.writableDatabase.update(
            TABLE_TRANSACTIONS,
            updated.toValues(),
            "id = ?",
            arrayOf(updated.id)
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        database.writableDatabase.delete(TABLE_TRANSACTIONS, "id = ?", arrayOf(id))
    }

    suspend fun getLatest(limit: Long = 50): List<Transaction> = withContext(Dispatchers.IO) {
        queryTransactions(
            selection = null,
            selectionArgs = null,
            orderBy = "date DESC",
            limit = limit.toString()
        )
    }

    suspend fun getAll(): List<Transaction> = getLatest(Long.MAX_VALUE)

    suspend fun getInRange(start: Long, end: Long): List<Transaction> = withContext(Dispatchers.IO) {
        queryTransactions(
            selection = "date >= ? AND date <= ?",
            selectionArgs = arrayOf(start.toString(), end.toString()),
            orderBy = "date DESC",
            limit = null
        )
    }

    suspend fun importAll(items: List<Transaction>): Int = withContext(Dispatchers.IO) {
        val db = database.writableDatabase
        var imported = 0
        db.beginTransaction()
        try {
            items.forEach { transaction ->
                val normalized = transaction.copy(
                    id = transaction.id.ifBlank { UUID.randomUUID().toString() }
                )
                db.insertWithOnConflict(
                    TABLE_TRANSACTIONS,
                    null,
                    normalized.toValues(),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                imported++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        imported
    }

    private fun queryTransactions(
        selection: String?,
        selectionArgs: Array<String>?,
        orderBy: String,
        limit: String?
    ): List<Transaction> {
        return database.readableDatabase.query(
            TABLE_TRANSACTIONS,
            COLUMNS,
            selection,
            selectionArgs,
            null,
            null,
            orderBy,
            limit
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        Transaction(
                            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                            type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                            amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                            category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                            note = cursor.getString(cursor.getColumnIndexOrThrow("note")),
                            date = cursor.getLong(cursor.getColumnIndexOrThrow("date")),
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
                        )
                    )
                }
            }
        }
    }
}

private class BudgetDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRANSACTIONS (
                id TEXT PRIMARY KEY NOT NULL,
                type TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                note TEXT NOT NULL,
                date INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_transactions_date ON $TABLE_TRANSACTIONS(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    companion object {
        @Volatile
        private var instance: BudgetDatabase? = null

        fun getInstance(context: Context): BudgetDatabase = instance ?: synchronized(this) {
            instance ?: BudgetDatabase(context).also { instance = it }
        }
    }
}

private fun Transaction.toValues() = ContentValues().apply {
    put("id", id)
    put("type", type)
    put("amount", amount)
    put("category", category)
    put("note", note)
    put("date", date)
    put("created_at", createdAt)
    put("updated_at", updatedAt)
}

private const val DATABASE_NAME = "my_budget.db"
private const val DATABASE_VERSION = 1
private const val TABLE_TRANSACTIONS = "transactions"
private val COLUMNS = arrayOf(
    "id",
    "type",
    "amount",
    "category",
    "note",
    "date",
    "created_at",
    "updated_at"
)
