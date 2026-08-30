package content.amilious.pet

import content.amilious.food.FoodKind
import content.amilious.food.FoodTable
import core.api.sendMessage
import core.game.interaction.IntType
import core.game.interaction.InteractionListener
import core.game.node.Node
import core.game.node.entity.player.Player
import core.game.node.item.Item
import core.game.world.update.flag.context.Animation
import core.game.world.update.flag.context.Graphics
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyListener : InteractionListener {

    private val ids = intArrayOf(MonkeyConfig.NPC_ID)

    private val alcoholIds = intArrayOf(
        1917, // Beer
        1905, // Asgarnian ale
        2955, // Moonlight mead
        1993, // Jug of wine
        7919  // Bottle of wine
    )

    override fun defineListeners() {
        on(ids, IntType.NPC, "pick-up", "pickup", "pack") { player, node ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || live !== node) {
                sendMessage(player, "That is not your monkey.")
                return@on true
            }
            live.openBagUi()
            true
        }

        on(ids, IntType.NPC, "talk-to", "talk to") { player, node ->
            sendMessage(player, "Gigos chatters and looks at you.")
            true
        }

        on(ids, IntType.NPC, "pet") { player, node ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || live !== node) {
                sendMessage(player, "That is not your monkey.")
                return@on true
            }
            player.face(live)
            player.animate(Animation(827))
            sendMessage(player, "You scratch Gigos behind the ear.")
            true
        }

        on(ids, IntType.NPC, "empty") { player, node ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || live !== node) {
                sendMessage(player, "That is not your monkey.")
                return@on true
            }
            val n = live.takeNonBananasToOwner()
            sendMessage(
                player,
                if (n == 0) "Gigos has nothing to empty (bananas stay)."
                else "You empty Gigos' pack. Bananas stay with him."
            )
            true
        }

        on(ids, IntType.NPC, "dismiss") { player, node ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || live !== node) {
                sendMessage(player, "That is not your monkey.")
                return@on true
            }
            live.dismiss()
            true
        }

        onUseWith(IntType.NPC, MonkeyConfig.BANANA_ID, *ids) { player, used, with ->
            useBanana(player, used.id, with)
        }
        onUseWith(IntType.NPC, 1964, *ids) { player, used, with ->
            useBanana(player, used.id, with)
        }

        onUseWith(IntType.NPC, alcoholIds, *ids) { player, used, with ->
            drinkAlcohol(player, used.id, with)
        }

        onUseWith(IntType.NPC, Int.MAX_VALUE, *ids) { player, used, with ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || with.id != MonkeyConfig.NPC_ID) {
                return@onUseWith false
            }
            if (live.isBananaItem(Item(used.id, 1))) {
                return@onUseWith false
            }
            if (used.id in alcoholIds) {
                return@onUseWith drinkAlcohol(player, used.id, with)
            }
            val item = Item(used.id, 1)
            if (!live.bag.hasSpaceFor(item)) {
                sendMessage(player, "Gigos cannot carry any more.")
                return@onUseWith true
            }
            if (player.inventory.remove(item) && live.bag.add(item)) {
                live.saveBag()
                sendMessage(player, "Gigos stuffs the ${item.name.lowercase()} in his pack.")
            }
            true
        }
    }

    private fun drinkAlcohol(player: Player, usedId: Int, with: Node): Boolean {
        val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
        if (live == null || with.id != MonkeyConfig.NPC_ID) {
            sendMessage(player, "That is not your monkey.")
            return true
        }
        val entry = FoodTable.get(usedId)
        if (entry == null || FoodKind.ALCOHOL !in entry.kinds) {
            sendMessage(player, "Gigos sniffs it and loses interest.")
            return true
        }
        val one = Item(usedId, 1)
        if (!player.inventory.remove(one)) return true
        if (entry.leftover > 0) player.inventory.add(Item(entry.leftover, 1))
        live.addDrunk(MonkeyConfig.DRUNK_BEER + entry.healMin * 2)
        live.graphics(Graphics(277, 80))
        sendMessage(player, "Gigos drinks ${a(entry.name)} and is getting crazy!")
        GigosHudPacket.send(player, live)
        return true
    }

    private fun useBanana(player: Player, usedId: Int, with: Node): Boolean {
        val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
        if (live == null || with !== live) {
            sendMessage(player, "That is not your monkey.")
            return true
        }
        val one = Item(usedId, 1)
        if (live.hunger() >= MonkeyConfig.HUNGER_MAX) {
            if (!player.inventory.remove(one)) return true
            if (live.addBananasNoted(1)) {
                live.saveBag()
                sendMessage(player, "Gigos stores the banana for later.")
            }
            return true
        }
        if (player.inventory.remove(one)) {
            live.addHunger(MonkeyConfig.HUNGER_BANANA)
            GigosHudPacket.send(player, live)
            sendMessage(player, "Gigos grabs the banana. Ook!")
        }
        return true
    }

    private fun a(name: String): String {
        val n = name.trim().lowercase()
        val an = n.firstOrNull() in setOf('a', 'e', 'i', 'o', 'u')
        return if (an) "an $n" else "a $n"
    }
}