package content.amilious.food

data class FoodFilter(
    val include: Set<FoodKind>? = null,
    val exclude: Set<FoodKind> = emptySet()
) {
    fun allows(entry: FoodEntry): Boolean {
        if (exclude.any { it in entry.kinds }) return false
        if (include != null && include.none { it in entry.kinds }) return false
        return true
    }

    companion object {
        /** Combat food: no beer, no potions, no stat pies. */
        val PLAIN = FoodFilter(exclude = setOf(FoodKind.ALCOHOL, FoodKind.POTION, FoodKind.BUFF))
        val FOOD_ONLY = FoodFilter(include = setOf(FoodKind.FOOD), exclude = setOf(FoodKind.ALCOHOL, FoodKind.POTION))
        val ANY = FoodFilter()
        val ALCOHOL = FoodFilter(include = setOf(FoodKind.ALCOHOL))
        val POTION = FoodFilter(include = setOf(FoodKind.POTION))
    }
}