package content.amilious.pet

import core.api.InputType
import core.api.sendInputDialogue
import core.api.sendMessage
import core.game.container.Container
import core.game.interaction.InterfaceListener
import core.game.node.entity.player.Player
import core.game.node.item.Item
import core.plugin.Initializable
import kotlin.math.min

@Initializable
class AmiliousMonkeyBagUi : InterfaceListener {

    override fun defineInterfaceListeners() {
        on(665, 0) { player, _, opcode, _, slot, _ ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on false
            val invItem = player.inventory.get(slot) ?: return@on true
            val want = amountFor(opcode)
            if (want < 0) {
                askAmount(player, live, player.inventory, live.bag, invItem.id)
                return@on true
            }
            val moved = transfer(player, live, player.inventory, live.bag, invItem.id, want)
            if (moved == 0) sendMessage(player, "Gigos cannot carry any more.")
            else {
                live.saveBag()
                live.openBagUi()
            }
            true
        }

        on(671, 27) { player, _, opcode, _, slot, _ ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on false
            val bagItem = live.bag.get(slot) ?: return@on true
            val want = amountFor(opcode)
            if (want < 0) {
                askAmount(player, live, live.bag, player.inventory, bagItem.id)
                return@on true
            }
            val moved = transfer(player, live, live.bag, player.inventory, bagItem.id, want)
            if (moved == 0) sendMessage(player, "You have no inventory space.")
            else {
                live.saveBag()
                live.openBagUi()
            }
            true
        }

        on(671) { player, _, _, button, _, _ ->
            if (button != 143974685) return@on false
            takeBob(player)
        }
    }

    private fun takeBob(player: Player): Boolean {
        val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            ?: return false
        val n = live.takeNonBananasToOwner()
        if (n == 0) sendMessage(player, "Gigos has nothing to give (bananas stay).")
        else sendMessage(player, "You take items from Gigos' pack.")
        return true
    }

    private fun askAmount(
        player: Player,
        live: AmiliousMonkey,
        from: Container,
        to: Container,
        id: Int
    ) {
        sendInputDialogue(player, InputType.AMOUNT, "Enter the amount:") { value ->
            val n = when (value) {
                is Int -> value
                is Number -> value.toInt()
                else -> value.toString().toIntOrNull() ?: 0
            }
            if (n <= 0) return@sendInputDialogue
            val moved = transfer(player, live, from, to, id, n)
            if (moved == 0) sendMessage(player, "Could not move that many.")
            else {
                live.saveBag()
                live.openBagUi()
            }
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

    private fun transfer(
        player: Player,
        live: AmiliousMonkey,
        from: Container,
        to: Container,
        id: Int,
        want: Int
    ): Int {
        if (want <= 0) return 0
        val n = min(want, from.getAmount(id))
        if (n <= 0) return 0
        val banana = id == MonkeyConfig.BANANA_ID || id == live.bananaNoteId()
        if (banana && to === live.bag) {
            if (!from.remove(Item(id, n))) return 0
            return if (live.addBananasNoted(n)) n else 0
        }
        val move = Item(id, n)
        if (!to.hasSpaceFor(move)) return 0
        if (from.remove(move) && to.add(move)) return n
        return 0
    }
}