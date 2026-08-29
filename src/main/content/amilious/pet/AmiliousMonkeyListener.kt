package content.amilious.pet

import core.api.sendMessage
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
        on(ids, IntType.NPC, "pick-up", "pickup", "pack", "interact") { player, node ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || live !== node) {
                sendMessage(player, "That is not your monkey.")
                return@on true
            }
            live.openBagUi()
            true
        }

        on(ids, IntType.NPC, "talk-to", "talk to") { player, node ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || live !== node) {
                sendMessage(player, "That is not your monkey.")
                return@on true
            }
            sendMessage(player, "Gigos chatters and looks at you.")
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
            if (player.inventory.remove(Item(MonkeyConfig.BANANA_ID, 1))) {
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