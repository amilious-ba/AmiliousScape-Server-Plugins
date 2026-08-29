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
        val hp = monkey.skills.lifepoints
        val hpMax = monkey.skills.maximumLifepoints.coerceAtLeast(1)
        val buf = IoBuffer(OPCODE, PacketHeader.BYTE)
        buf.p1(1)
        buf.p2(hp.coerceIn(0, 65535))
        buf.p2(hpMax.coerceIn(1, 65535))
        buf.putString("Gigos")
        buf.p1(1)
        buf.p1(2) // same id as ::gigosop 2
        buf.p1(if (lootOn) 1 else 0)
        buf.putString("Autoloot")
        player.session.write(buf)
    }
}