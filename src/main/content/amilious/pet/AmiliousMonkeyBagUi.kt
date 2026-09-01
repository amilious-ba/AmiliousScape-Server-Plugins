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
                ?: return@on true
            val invItem = player.inventory.get(slot) ?: return@on true
            val want = amountFor(opcode)
            if (want < 0) {
                askAmount(player, live, player.inventory, live.bag, invItem.id)
                return@on true
            }
            val moved = if (live.isBananaItem(invItem) || invItem.id == MonkeyConfig.BANANA_ID) {
                depositBananas(player, live, want)
            } else {
                transfer(player, live, player.inventory, live.bag, invItem.id, want)
            }
            if (moved == 0) sendMessage(player, "Gigos cannot carry any more.")
            else {
                live.saveBag()
                live.openBagUi()
                if (live.isBananaItem(invItem) || invItem.id == MonkeyConfig.BANANA_ID) {
                    sendMessage(player, "Gigos notes the bananas.")
                }
            }
            true
        }

        on(671, 27) { player, _, opcode, _, slot, _ ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on true
            if (slot < 0) return@on true
            val bagItem = live.bag.get(slot) ?: return@on true
            val want = amountFor(opcode)
            if (want < 0) {
                askAmount(player, live, live.bag, player.inventory, bagItem.id)
                return@on true
            }
            val room = player.inventory.getMaximumAdd(bagItem)
            if (room <= 0) {
                sendMessage(player, "You have no inventory space.")
                return@on true
            }
            val move = Item(bagItem.id, min(want, min(bagItem.amount, room)))
            if (live.bag.remove(move) && player.inventory.add(move)) {
                live.saveBag()
                live.openBagUi()
                if (move.amount < bagItem.amount && want == Int.MAX_VALUE) {
                    sendMessage(player, "Inventory full. Left the rest with Gigos.")
                }
            }
            true
        }

        on(671, 29) { player, _, _, _, _, _ ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
                ?: return@on false
            val n = takeNonBananas(player, live)
            live.saveBag()
            live.openBagUi()
            if (n == 0) sendMessage(player, "Gigos has nothing to give (bananas stay).")
            else sendMessage(player, "You take items from Gigos' pack.")
            true
        }
    }

    private fun depositBananas(player: Player, live: AmiliousMonkey, want: Int): Int {
        val fresh = MonkeyConfig.BANANA_ID
        val noted = noteId()
        val have = player.inventory.getAmount(fresh) + player.inventory.getAmount(noted)
        if (have <= 0 || want <= 0) return 0
        val n = min(want, have)
        var left = n
        val takeNoted = min(left, player.inventory.getAmount(noted))
        if (takeNoted > 0 && player.inventory.remove(Item(noted, takeNoted))) left -= takeNoted
        val takeFresh = min(left, player.inventory.getAmount(fresh))
        if (takeFresh > 0 && player.inventory.remove(Item(fresh, takeFresh))) left -= takeFresh
        val got = n - left
        if (got <= 0) return 0
        if (live.addBananasNoted(got)) return got
        player.inventory.add(Item(noted, got))
        return 0
    }

    private fun depositGigos(player: Player): Boolean {
        val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            ?: return false
        val n = live.depositBagToBank(keepBananas = true)
        sendMessage(
            player,
            if (n == 0) "Gigos has nothing to deposit (bananas stay)."
            else "Gigos banks his pack. Bananas stay with him."
        )
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

    private fun noteId(): Int {
        val def = Item(MonkeyConfig.BANANA_ID).definition
        val n = def.noteId
        return if (n > 0) n else 1964
    }

    private fun isBanana(id: Int): Boolean =
        id == MonkeyConfig.BANANA_ID || id == noteId()

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
        if (isBanana(id) && to === live.bag) {
            if (!from.remove(Item(id, n))) return 0
            val noted = Item(noteId(), n)
            return if (live.bag.add(noted)) n else 0
        }
        val move = Item(id, n)
        if (!to.hasSpaceFor(move)) return 0
        if (from.remove(move) && to.add(move)) return n
        return 0
    }

    private fun takeNonBananas(player: Player, live: AmiliousMonkey): Int {
        var moved = 0
        for (item in live.bag.toArray()) {
            if (item == null) continue
            if (isBanana(item.id)) continue
            val room = player.inventory.getMaximumAdd(item)
            if (room <= 0) {
                sendMessage(player, "Inventory full. Left the rest with Gigos.")
                break
            }
            val move = Item(item.id, min(item.amount, room))
            if (live.bag.remove(move) && player.inventory.add(move)) moved++
            else {
                sendMessage(player, "Inventory full. Left the rest with Gigos.")
                break
            }
        }
        return moved
    }
}