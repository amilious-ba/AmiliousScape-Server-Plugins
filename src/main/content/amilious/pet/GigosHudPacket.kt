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
        val buf = IoBuffer(OPCODE, PacketHeader.BYTE)
        buf.p1(1)
        buf.p2(monkey.hunger().coerceIn(0, 65535))
        buf.p2(MonkeyConfig.HUNGER_MAX)
        buf.putString("Gigos")
        buf.p1(1)
        buf.p1(2)
        buf.p1(if (lootOn) 1 else 0)
        buf.putString("Autoloot")
        player.session.write(buf)
    }
}