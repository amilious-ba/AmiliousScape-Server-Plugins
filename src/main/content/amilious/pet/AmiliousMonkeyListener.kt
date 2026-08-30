package content.amilious.pet

import core.api.sendMessage
import core.api.setAttribute
import core.game.interaction.IntType
import core.game.interaction.InteractionListener
import core.game.node.Node
import core.game.node.entity.player.Player
import core.game.node.item.Item
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyListener : InteractionListener {

    private val ids = intArrayOf(MonkeyConfig.NPC_ID)

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
            player.animate(core.game.world.update.flag.context.Animation(827))
            sendMessage(player, "You scratch Gigos behind the ear.")
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

        onUseWith(IntType.NPC, Int.MAX_VALUE, *ids) { player, used, with ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null || with !== live) {
                return@onUseWith false
            }
            if (live.isBananaItem(Item(used.id, 1))) {
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
}