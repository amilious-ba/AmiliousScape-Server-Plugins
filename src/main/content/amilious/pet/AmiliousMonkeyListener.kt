package content.amilious.pet

import core.api.sendMessage
import core.api.setAttribute
import core.game.interaction.IntType
import core.game.interaction.InteractionListener
import core.game.node.item.Item
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyListener : InteractionListener {

    private val ids = intArrayOf(
        6943, 7211, 7213, 7215, 7217, 7219, 7221, 7223, 7225, 7227
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

        on(ids, IntType.NPC, "loot", "autoloot-on", "autoloot-off") { player, node ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || live !== node) {
                sendMessage(player, "That is not your monkey.")
                return@on true
            }
            val on = player.getAttribute(MonkeyConfig.ATTR_LOOT, true)
            setAttribute(player, MonkeyConfig.ATTR_LOOT, !on)
            live.refreshMenu()
            sendMessage(player, if (!on) "Gigos will loot your kills." else "Gigos will not loot.")
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

        onUseWith(IntType.NPC, MonkeyConfig.BANANA_ID, *ids) { player, _, with ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || with !== live) {
                sendMessage(player, "That is not your monkey.")
                return@onUseWith true
            }
            val banana = Item(MonkeyConfig.BANANA_ID, 1)
            if (live.hunger() >= MonkeyConfig.HUNGER_MAX) {
                if (!live.bag.hasSpaceFor(banana)) {
                    sendMessage(player, "Gigos is full and his pack has no room.")
                    return@onUseWith true
                }
                if (player.inventory.remove(banana) && live.addBananasNoted(1)) {
                    live.saveBag()
                    sendMessage(player, "Gigos stores the banana for later.")
                }
                return@onUseWith true
            }
            if (player.inventory.remove(banana)) {
                live.addHunger(MonkeyConfig.HUNGER_BANANA)
                GigosHudPacket.send(player, live)
                sendMessage(player, "Gigos grabs the banana. Ook!")
            }
            true
        }

        onUseWith(IntType.NPC, Int.MAX_VALUE, *ids) { player, used, with ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || with !== live) {
                return@onUseWith false
            }
            if (used.id == MonkeyConfig.BANANA_ID) {
                return@onUseWith false
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
}