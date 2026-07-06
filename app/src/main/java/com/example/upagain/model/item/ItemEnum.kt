package com.example.upagain.model.item

enum class ItemStatus(val value: String) {
    PENDING("pending"), APPROVED("approved"), REFUSED("refused");

    companion object {
        fun fromValue(value: String): ItemStatus? {
            return entries.find { it.value == value }
        }
    }
}

enum class ItemMaterial(val value: String) {
    PLASTIC("plastic"), METAL("metal"), GLASS("glass"), WOOD("wood"), TEXTILE("textile"), OTHER("other");

    companion object {
        fun fromValue(value: String): ItemMaterial? {
            return entries.find { it.value == value }
        }
    }
}

enum class ItemSortOption(val value: String) {
    MOST_RECENT_CREATION("most_recent_creation"), OLDEST_CREATION("oldest_creation"), LOWEST_PRICE("lowest_price"), HIGHEST_PRICE(
        "highest_price"
    );

    companion object {
        fun fromValue(value: String): ItemSortOption? {
            return entries.find { it.value == value }
        }
    }
}