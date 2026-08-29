package content.amilious.pet

import core.api.sendMessage
import core.game.container.Container
import core.game.interaction.InterfaceListener
import core.game.node.item.Item
import core.plugin.Initializable
import kotlin.math.min

@Initializable
class AmiliousMonkeyBagUi : InterfaceListener {

    override fun defineInterfaceListeners() {
        on(665, 0) { player, _, opcode, _, slot, _ ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on true
            val invItem = player.inventory.get(slot) ?: return@on true
            if (invItem.id == MonkeyConfig.BANANA_ID) return@on true
            val want = amountFor(opcode)
            if (want < 0) {
                sendMessage(player, "Store-X not wired yet. Use Store-1/5/10/All.")
                return@on true
            }
            val moved = transfer(player.inventory, live.bag, invItem.id, want)
            if (moved == 0) {
                sendMessage(player, "Gigos cannot carry any more.")
            } else {
                live.saveBag()
                live.openBagUi()
            }
            true
        }

        on(671, 27) { player, _, opcode, _, slot, _ ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on true
            val bagItem = live.bag.get(slot) ?: return@on true
            val want = amountFor(opcode)
            if (want < 0) {
                sendMessage(player, "Withdraw-X not wired yet. Use Withdraw-1/5/10/All.")
                return@on true
            }
            val moved = transfer(live.bag, player.inventory, bagItem.id, want)
            if (moved == 0) {
                sendMessage(player, "You have no inventory space.")
            } else {
                live.saveBag()
                live.openBagUi()
            }
            true
        }
    }

    private fun amountFor(opcode: Int): Int = when (opcode) {
        155 -> 1
        196 -> 5
        124 -> 10
        199 -> Int.MAX_VALUE
        234 -> -1
        else -> 1
    }

    private fun transfer(from: Container, to: Container, id: Int, want: Int): Int {
        if (want <= 0) return 0
        val n = min(want, from.getAmount(id))
        if (n <= 0) return 0
        val move = Item(id, n)
        if (!to.hasSpaceFor(move)) return 0
        if (from.remove(move) && to.add(move)) return n
        return 0
    }
}