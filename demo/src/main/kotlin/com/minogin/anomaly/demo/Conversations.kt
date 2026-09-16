package com.minogin.anomaly.demo

/** What a customer message is really about. The scripted model "knows" this; a real model would guess. */
enum class Category(val label: String) {
    SUPPORT("support"),
    SALES("sales"),
    FEEDBACK("feedback"),
}

data class Conversation(val id: Int, val category: Category, val message: String)

/** Ten invented customer messages: 3 support, 3 sales, 4 feedback. */
val CONVERSATIONS = listOf(
    Conversation(1, Category.SUPPORT, "My login code never arrives"),
    Conversation(2, Category.SALES, "Do you offer a discount for annual plans?"),
    Conversation(3, Category.FEEDBACK, "Love the new dashboard, great work"),
    Conversation(4, Category.SUPPORT, "The app crashes when I open settings"),
    Conversation(5, Category.FEEDBACK, "The export feature is confusing"),
    Conversation(6, Category.SALES, "What does the team plan cost per month?"),
    Conversation(7, Category.FEEDBACK, "Please bring back the old layout"),
    Conversation(8, Category.SUPPORT, "How do I reset my password?"),
    Conversation(9, Category.SALES, "Can I get a quote for 50 seats?"),
    Conversation(10, Category.FEEDBACK, "Dark mode is fantastic"),
)
