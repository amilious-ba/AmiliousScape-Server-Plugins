package content.amilious.pet

import core.game.node.entity.player.Player
import core.net.packet.IoBuffer
import core.net.packet.PacketHeader

object GigosHudPacket {

    const val OPCODE = 201

    fun hide(player: Player) {
        val buf = IoBuffer(OPCODE, PacketHeader.BYTE)
        buf.p1(0)
        player.session.write(buf)
    }

    fun send(player: Player, monkey: AmiliousMonkey) {
        val lootOn = player.getAttribute(MonkeyConfig.ATTR_LOOT, true)
        val dungOn = player.getAttribute(MonkeyConfig.ATTR_DUNG, true)
        val eatOn = player.getAttribute(MonkeyConfig.ATTR_EAT, true)
        val b2bOn = player.getAttribute(MonkeyConfig.ATTR_B2B, true)
        val feedOn = player.getAttribute(MonkeyConfig.ATTR_FEED, true)
        val buf = IoBuffer(OPCODE, PacketHeader.BYTE)
        buf.p1(1)
        buf.p2(monkey.hunger().coerceIn(0, 65535))
        buf.p2(MonkeyConfig.HUNGER_MAX)
        buf.putString("Gigos")
        buf.p2(monkey.bananaCount().coerceIn(0, 65535))
        buf.putString(monkey.brainActionName())
        buf.p1(5) //<- number of toggles
        buf.p1(2)
        buf.p1(if (lootOn) 1 else 0)
        buf.putString("Autoloot")
        buf.p1(4)
        buf.p1(if (dungOn) 1 else 0)
        buf.putString("Dung")
        buf.p1(5)
        buf.p1(if (eatOn) 1 else 0)
        buf.putString("Auto-eat")
        buf.p1(6)
        buf.p1(if (b2bOn) 1 else 0)
        buf.putString("Bones2B")
        buf.p1(7)
        buf.p1(if (feedOn) 1 else 0)
        buf.putString("Feed")
        player.session.write(buf)
    }
}