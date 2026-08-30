package content.amilious.pet

import core.api.sendMessage
import core.game.interaction.InterfaceListener
import core.game.node.entity.player.Player
import core.game.node.item.Item
import core.plugin.Initializable
import kotlin.math.min

@Initializable
class AmiliousMonkeyBagUi : InterfaceListener {

    override fun defineInterfaceListeners() {
        on(665, 0) { player, _, opcode, _, slot, _ ->
            sendMessage(player, "store opcode=$opcode slot=$slot")
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on true
            val invItem = player.inventory.get(slot) ?: return@on true
            if (invItem.id == MonkeyConfig.BANANA_ID) return@on true
            val want = amountFor(opcode)
            if (want < 0) {
                sendMessage(player, "Store-X not wired yet. Use Store-1/5/10/All.")
                return@on true
            }
            val move = Item(invItem.id, min(want, invItem.amount))
            if (!live.bag.hasSpaceFor(move)) {
                sendMessage(player, "Gigos cannot carry any more.")
                return@on true
            }
            if (player.inventory.remove(move) && live.bag.add(move)) {
                live.saveBag()
                live.openBagUi()
            }
            true
        }

        on(671, 27) { player, _, opcode, _, slot, _ ->
            sendMessage(player, "take opcode=$opcode slot=$slot")
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on true
            if (slot < 0) return@on true
            val bagItem = live.bag.get(slot) ?: return@on true
            val want = amountFor(opcode)
            if (want < 0) {
                sendMessage(player, "Withdraw-X not wired yet. Use Withdraw-1/5/10/All.")
                return@on true
            }
            val move = Item(bagItem.id, min(want, bagItem.amount))
            if (!player.inventory.hasSpaceFor(move)) {
                sendMessage(player, "You have no inventory space.")
                return@on true
            }
            if (live.bag.remove(move) && player.inventory.add(move)) {
                live.saveBag()
                live.openBagUi()
            }
            true
        }

        on(671, 29) { player, _, opcode, _, slot, _ ->
            sendMessage(player, "take-all opcode=$opcode slot=$slot")
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on true
            takeAll(player, live)
            true
        }
    }

    private fun takeAll(player: Player, live: AmiliousMonkey) {
        var moved = 0
        for (item in live.bag.toArray()) {
            if (item == null) continue
            val room = player.inventory.getMaximumAdd(item)
            if (room <= 0) {
                sendMessage(player, "Inventory full. Left the rest with Gigos.")
                break
            }
            val move = Item(item.id, min(item.amount, room))
            if (live.bag.remove(move) && player.inventory.add(move)) {
                moved++
            } else {
                sendMessage(player, "Inventory full. Left the rest with Gigos.")
                break
            }
        }
        live.saveBag()
        live.openBagUi()
        sendMessage(
            player,
            if (moved == 0) "Gigos is not carrying anything." else "You take Gigos' pack."
        )
    }

    private fun amountFor(opcode: Int): Int = when (opcode) {
        155 -> 1
        196 -> 5
        124 -> 10
        199 -> Int.MAX_VALUE
        234 -> -1
        else -> 1
    }
}