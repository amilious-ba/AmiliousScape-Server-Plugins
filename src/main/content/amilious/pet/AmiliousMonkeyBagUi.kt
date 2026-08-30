package content.amilious.pet

import core.api.sendMessage
import core.game.interaction.InterfaceListener
import core.game.node.item.Item
import core.plugin.Initializable
import kotlin.math.min

@Initializable
class AmiliousMonkeyBagUi : InterfaceListener {

    override fun defineInterfaceListeners() {
        on(665) { player, _, opcode, button, slot, itemId ->
            handle(player, store = true, opcode, button, slot, itemId)
        }
        on(671) { player, _, opcode, button, slot, itemId ->
            handle(player, store = false, opcode, button, slot, itemId)
        }
    }

    private fun handle(
        player: core.game.node.entity.player.Player,
        store: Boolean,
        opcode: Int,
        button: Int,
        slot: Int,
        itemId: Int
    ): Boolean {
        val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            ?: return true
        sendMessage(
            player,
            "${if (store) "STORE" else "TAKE"} op=$opcode btn=$button slot=$slot item=$itemId"
        )
        val from = if (store) player.inventory else live.bag
        val to = if (store) live.bag else player.inventory
        val src = from.get(slot) ?: return true
        if (store && src.id == MonkeyConfig.BANANA_ID) return true
        val want = amountFor(opcode)
        if (want < 0) {
            sendMessage(player, "X not wired. Use 1/5/10/All.")
            return true
        }
        val move = Item(src.id, min(want, src.amount))
        if (!to.hasSpaceFor(move)) {
            sendMessage(player, if (store) "Gigos cannot carry any more." else "You have no inventory space.")
            return true
        }
        if (from.remove(move) && to.add(move)) {
            live.saveBag()
            live.openBagUi()
        }
        return true
    }

    private fun amountFor(opcode: Int): Int = when (opcode) {
        155, 117 -> 1
        196, 43, 145 -> 5
        124, 129, 226 -> 10
        199, 135, 20, 169 -> Int.MAX_VALUE
        234, 9, 166 -> -1
        else -> 1
    }
}