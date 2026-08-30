package content.amilious.pet

import core.api.playAudio
import core.api.sendMessage
import core.game.interaction.InterfaceListener
import core.plugin.Initializable

@Initializable
class GigosBankDeposit : InterfaceListener {

    override fun defineInterfaceListeners() {
        // Bank window (530 is interface 12). Log children, then bind the BoB button.
        on(12) { player, _, opcode, button, slot, itemId ->
            sendMessage(player, "bank iface btn=$button op=$opcode slot=$slot item=$itemId")
            false
            if (button == BOB_BUTTON) {
                return@on deposit(player)
            }
            false
        }
    }

    companion object {
        // set after one debug click; common 530 values are 15 / 16 / 17
        const val BOB_BUTTON = 16

        fun deposit(player: core.game.node.entity.player.Player): Boolean {
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null) return false
            val n = live.depositBagToBank(keepBananas = true)
            sendMessage(
                player,
                if (n == 0) "Gigos has nothing to deposit (bananas stay)."
                else "Gigos banks his pack. Bananas stay with him."
            )
            return true
        }
    }
}