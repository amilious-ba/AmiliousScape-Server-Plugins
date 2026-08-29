package content.amilious.pet

import core.api.sendMessage
import core.game.interaction.IntType
import core.game.interaction.InteractionListener
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyListener : InteractionListener {

    // cover default + color variants so Pick-up works after you change NPC_ID
    private val ids = intArrayOf(
        6943, 7211, 7213, 7215, 7217, 7219, 7221, 7223, 7225, 7227
    )

    override fun defineListeners() {
        on(ids, IntType.NPC, "pick-up", "pickup", "talk-to", "talk to", "interact") { player, node ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || live !== node) {
                sendMessage(player, "That is not your monkey.")
                return@on true
            }
            when (player.getAttribute("interact:option", "").lowercase()) {
                "talk-to", "talk to", "interact" -> {
                    sendMessage(player, "Gigos chatters and looks at you.")
                }
                else -> {
                    live.dismiss()
                }
            }
            true
        }

        onUseWith(IntType.NPC, Int.MAX_VALUE, *ids) { player, used, with ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || with.id != MonkeyConfig.NPC_ID) {
                return@onUseWith false
            }
            val item = used.asItem()
            if (item.id == MonkeyConfig.BANANA_ID) {
                return@onUseWith false // let the banana handler run
            }
            if (!player.inventory.contains(item.id, item.amount)) {
                return@onUseWith true
            }
            if (!live.bag.hasSpaceFor(item)) {
                sendMessage(player, "Gigos cannot carry any more.")
                return@onUseWith true
            }
            if (player.inventory.remove(item) && live.bag.add(item)) {
                sendMessage(player, "Gigos stuffs the ${item.name.lowercase()} in his pack.")
            }
            true
        }

    }
}