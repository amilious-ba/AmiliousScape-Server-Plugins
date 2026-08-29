package content.amilious.pet

import core.api.sendMessage
import core.game.interaction.InterfaceListener
import core.game.node.item.Item
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyBagUi : InterfaceListener {

    override fun defineInterfaceListeners() {
        // Player inventory in the BoB window (665, child 0)
        on(665, 0) { player, _, opcode, _, slot, _ ->
            sendMessage(player, "store opcode=$opcode slot=$slot")
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on true
            val invItem = player.inventory.get(slot) ?: return@on true
            if (invItem.id == MonkeyConfig.BANANA_ID) return@on true
            val move = Item(invItem.id, 1)
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
            val bagItem = live.bag.get(slot) ?: return@on true
            val move = Item(bagItem.id, 1)
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


    }
}