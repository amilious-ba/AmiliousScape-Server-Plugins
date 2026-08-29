package content.amilious.pet

import core.game.node.entity.npc.NPC
import core.game.node.entity.player.Player
import core.net.packet.IoBuffer
import core.net.packet.PacketHeader

object NpcMenuPacket {
    const val OPCODE = 200

    fun send(player: Player, npc: NPC, displayName: String, vararg options: Pair<Int, String>) {
        val session = player.session ?: return
        val buf = IoBuffer(OPCODE, PacketHeader.SHORT)
        buf.putShort(npc.index)
        buf.putShort(npc.id)
        buf.put(0) // flags: 0 = set
        buf.putString(displayName)
        buf.put(options.size)
        for ((slot, text) in options) {
            buf.put(slot)
            buf.putString(text)
        }
        buf.cypherOpcode(session.isaacPair.output)
        session.write(buf)
    }

    fun clear(player: Player, npc: NPC) {
        val session = player.session ?: return
        val buf = IoBuffer(OPCODE, PacketHeader.SHORT)
        buf.putShort(npc.index)
        buf.putShort(npc.id)
        buf.put(1) // flags: bit0 = clear
        buf.putString("")
        buf.put(0)
        buf.cypherOpcode(session.isaacPair.output)
        session.write(buf)
    }
}