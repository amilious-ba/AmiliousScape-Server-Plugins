package content.amilious.food

import core.game.container.Container
import core.game.node.entity.player.Player
import core.game.node.item.Item

object FoodTable {

    private val ALL: List<FoodEntry> = listOf(
        // fish / seafood
        e("Anchovies", 319, 1),
        e("Shrimps", 315, 3),
        e("Sardine", 325, 4),
        e("Herring", 347, 5),
        e("Mackerel", 355, 6),
        e("Trout", 333, 7),
        e("Cod", 339, 7),
        e("Pike", 351, 8),
        e("Salmon", 329, 9),
        e("Tuna", 361, 10),
        e("Rainbow fish", 10136, 11),
        e("Lobster", 379, 12),
        e("Bass", 365, 13),
        e("Swordfish", 373, 14),
        e("Monkfish", 7946, 16),
        e("Cooked karambwan", 3144, 18, kinds = setOf(FoodKind.FOOD, FoodKind.COMBO)),
        e("Shark", 385, 20),
        e("Sea turtle", 397, 21),
        e("Manta ray", 391, 22),

        // meat
        e("Cooked chicken", 2140, 3),
        e("Cooked meat", 2142, 3),
        e("Cooked rabbit", 3228, 5),
        e("Roast bird meat", 9980, 6),
        e("Roast rabbit", 7223, 7),
        e("Roast beast meat", 9988, 8),
        e("Cooked chompy", 2878, 10),
        e("Ugthanki kebab", 1883, 19),

        // bakery / veg
        e("Cabbage", 1965, 1),
        e("Potato", 1942, 1),
        e("Banana", 1963, 2),
        e("Cheese", 1985, 2),
        e("Bread", 2309, 5),
        e("Baked potato", 6701, 4),
        e("Potato with butter", 6703, 14),
        e("Potato with cheese", 6705, 16),
        e("Tuna potato", 7060, 22),
        e("Stew", 2003, 11),
        e("Curry", 2011, 19),

        // cake (4 per bite in 2009)
        e("Cake", 1891, 4, leftover = 1893, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("2/3 cake", 1893, 4, leftover = 1895, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Slice of cake", 1895, 4, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Chocolate cake", 1897, 5, leftover = 1899, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("2/3 chocolate cake", 1899, 5, leftover = 1901, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Chocolate slice", 1901, 5, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),

        // pizza
        e("Plain pizza", 2289, 7, leftover = 2291, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("1/2 plain pizza", 2291, 7, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Meat pizza", 2293, 8, leftover = 2295, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("1/2 meat pizza", 2295, 8, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Anchovy pizza", 2297, 9, leftover = 2299, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("1/2 anchovy pizza", 2299, 9, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Pineapple pizza", 2301, 11, leftover = 2303, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("1/2 pineapple pizza", 2303, 11, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),

        // pies (per bite)
        e("Redberry pie", 2325, 5, leftover = 2333, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Half redberry pie", 2333, 5, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Meat pie", 2327, 6, leftover = 2331, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Half meat pie", 2331, 6, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Apple pie", 2323, 7, leftover = 2335, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Half apple pie", 2335, 7, kinds = setOf(FoodKind.FOOD, FoodKind.MULTI)),
        e("Garden pie", 7178, 6, leftover = 7180, kinds = setOf(FoodKind.FOOD, FoodKind.BUFF, FoodKind.MULTI)),
        e("Fish pie", 7188, 6, leftover = 7190, kinds = setOf(FoodKind.FOOD, FoodKind.BUFF, FoodKind.MULTI)),
        e("Admiral pie", 7198, 8, leftover = 7200, kinds = setOf(FoodKind.FOOD, FoodKind.BUFF, FoodKind.MULTI)),
        e("Wild pie", 7208, 11, leftover = 7210, kinds = setOf(FoodKind.FOOD, FoodKind.BUFF, FoodKind.MULTI)),
        e("Summer pie", 7218, 11, leftover = 7220, kinds = setOf(FoodKind.FOOD, FoodKind.BUFF, FoodKind.MULTI)),

        // random
        e("Thin snail", 3369, 5, 7, kinds = setOf(FoodKind.FOOD, FoodKind.RANDOM)),
        e("Lean snail", 3371, 6, 8, kinds = setOf(FoodKind.FOOD, FoodKind.RANDOM)),
        e("Fat snail", 3373, 7, 9, kinds = setOf(FoodKind.FOOD, FoodKind.RANDOM)),
        e("Cooked slimy eel", 3381, 6, 10, kinds = setOf(FoodKind.FOOD, FoodKind.RANDOM)),
        e("Cave eel", 5003, 8, 12, kinds = setOf(FoodKind.FOOD, FoodKind.RANDOM)),
        e("Kebab", 1971, 0, 19, kinds = setOf(FoodKind.FOOD, FoodKind.RANDOM)),

        // alcohol (excluded by PLAIN)
        e("Beer", 1917, 1, kinds = setOf(FoodKind.ALCOHOL, FoodKind.BUFF)),
        e("Asgarnian ale", 1905, 2, kinds = setOf(FoodKind.ALCOHOL, FoodKind.BUFF)),
        e("Moonlight mead", 2955, 5, kinds = setOf(FoodKind.ALCOHOL)),
        e("Jug of wine", 1993, 11, kinds = setOf(FoodKind.ALCOHOL, FoodKind.BUFF)),
        e("Bottle of wine", 7919, 14, kinds = setOf(FoodKind.ALCOHOL, FoodKind.BUFF)),

        // potion that heals (excluded by PLAIN)
        e("Saradomin brew (4)", 6685, 16, leftover = 6687, kinds = setOf(FoodKind.POTION, FoodKind.BUFF, FoodKind.MULTI)),
        e("Saradomin brew (3)", 6687, 16, leftover = 6689, kinds = setOf(FoodKind.POTION, FoodKind.BUFF, FoodKind.MULTI)),
        e("Saradomin brew (2)", 6689, 16, leftover = 6691, kinds = setOf(FoodKind.POTION, FoodKind.BUFF, FoodKind.MULTI)),
        e("Saradomin brew (1)", 6691, 16, kinds = setOf(FoodKind.POTION, FoodKind.BUFF))
    )

    private val byId: Map<Int, FoodEntry> = HashMap<Int, FoodEntry>().apply {
        for (e in ALL) for (id in e.ids) put(id, e)
    }

    fun get(id: Int): FoodEntry? = byId[id]

    fun isFood(id: Int, filter: FoodFilter = FoodFilter.ANY): Boolean {
        val e = get(id) ?: return false
        return filter.allows(e)
    }

    fun missingHp(player: Player): Int {
        val max = player.skills.maximumLifepoints
        val now = player.skills.lifepoints
        return (max - now).coerceAtLeast(0)
    }

    /**
     * Best item in [bag] that heals as much as possible without going over [missingHp].
     * If nothing fits and [allowWaste] is true, pick the smallest overheal.
     */
    fun bestFor(
        bag: Container,
        missingHp: Int,
        filter: FoodFilter = FoodFilter.PLAIN,
        allowWaste: Boolean = false
    ): Pair<Item, FoodEntry>? {
        if (missingHp <= 0) return null
        var bestFit: Pair<Item, FoodEntry>? = null
        var bestFitHeal = -1
        var leastWaste: Pair<Item, FoodEntry>? = null
        var leastWasteAmt = Int.MAX_VALUE

        for (item in bag.toArray()) {
            if (item == null) continue
            val entry = get(item.id) ?: continue
            if (!filter.allows(entry)) continue
            if (entry.healMin <= 0) continue
            if (entry.fits(missingHp)) {
                if (entry.healMin > bestFitHeal) {
                    bestFitHeal = entry.healMin
                    bestFit = item to entry
                }
            } else if (allowWaste) {
                val w = entry.wasted(missingHp)
                if (w < leastWasteAmt) {
                    leastWasteAmt = w
                    leastWaste = item to entry
                }
            }
        }
        return bestFit ?: leastWaste
    }

    fun bestForPlayer(
        player: Player,
        bag: Container,
        filter: FoodFilter = FoodFilter.PLAIN,
        allowWaste: Boolean = false
    ): Pair<Item, FoodEntry>? = bestFor(bag, missingHp(player), filter, allowWaste)

    private fun e(
        name: String,
        id: Int,
        heal: Int,
        healMax: Int = heal,
        leftover: Int = -1,
        kinds: Set<FoodKind> = setOf(FoodKind.FOOD)
    ) = FoodEntry(name, intArrayOf(id), heal, healMax, leftover, kinds)
}