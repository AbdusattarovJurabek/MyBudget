package uz.mybudget.app.data.model

data class Transaction(
    val id: String = "",
    val type: String = "expense", // income yoki expense
    val amount: Double = 0.0,
    val category: String = "Boshqa",
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
