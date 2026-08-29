package content.amilious.pet

import core.game.interaction.InterfaceListener
import core.game.node.item.Item
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyBagUi : InterfaceListener {

    override fun defineInterfaceListeners() {
        // player inventory tab (665)
        on(665) { player, _, _, _, slot, itemId ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on true
            if (itemId <= 0) return@on true
            val invItem = player.inventory.get(slot) ?: return@on true
            if (invItem.id == MonkeyConfig.BANANA_ID) return@on true
            val move = Item(invItem.id, 1)
            if (!live.bag.hasSpaceFor(move)) {
                player.sendMessage("Gigos cannot carry any more.")
                return@on true
            }
            if (player.inventory.remove(move) && live.bag.add(move)) {
                live.saveBag()
                live.openBagUi()
            }
            true
        }

        // Gigos pack (671)
        on(671) { player, _, _, _, slot, itemId ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on true
            if (itemId <= 0) return@on true
            val bagItem = live.bag.get(slot) ?: return@on true
            val move = Item(bagItem.id, 1)
            if (!player.inventory.hasSpaceFor(move)) {
                player.sendMessage("You have no inventory space.")
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