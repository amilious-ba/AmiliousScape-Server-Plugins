package content.amilious.food

data class FoodEntry(
    val name: String,
    val ids: IntArray,
    val healMin: Int,
    val healMax: Int = healMin,
    val leftover: Int = -1,
    val kinds: Set<FoodKind> = setOf(FoodKind.FOOD)
) {
    fun matches(id: Int): Boolean = ids.any { it == id }
    fun wasted(missingHp: Int): Int = (healMin - missingHp).coerceAtLeast(0)
    fun fits(missingHp: Int): Boolean = healMin > 0 && healMin <= missingHp
}