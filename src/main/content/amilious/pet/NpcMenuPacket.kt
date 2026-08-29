package content.amilious.pet

import core.game.node.entity.npc.NPC
import core.game.node.entity.player.Player
import core.net.packet.IoBuffer
import core.net.packet.PacketHeader

object NpcMenuPacket {
    const val OPCODE = 200

    fun send(player: Player, npc: NPC, displayName: String, vararg options: Pair<Int, String>) {
        val session = player.session ?: return
        val buf = IoBuffer(OPCODE, PacketHeader.BYTE)
        buf.p2(npc.index)
        buf.p2(npc.id)
        buf.p1(0)
        buf.putString(displayName)
        buf.p1(options.size)
        for ((slot, text) in options) {
            buf.p1(slot)
            buf.putString(text)
        }
        buf.cypherOpcode(session.isaacPair.output)
        session.write(buf)
    }

    fun clear(player: Player, npc: NPC) {
        val session = player.session ?: return
        val buf = IoBuffer(OPCODE, PacketHeader.BYTE)
        buf.p2(npc.index)
        buf.p2(npc.id)
        buf.p1(1)
        buf.putString("")
        buf.p1(0)
        buf.cypherOpcode(session.isaacPair.output)
        session.write(buf)
    }
}