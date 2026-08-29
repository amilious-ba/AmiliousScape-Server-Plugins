package content.amilious.pet

import core.game.node.entity.npc.NPC
import core.game.node.entity.player.Player
import core.net.packet.IoBuffer
import core.net.packet.PacketHeader

object NpcMenuPacket {
    const val OPCODE = 200

    fun send(player: Player, npc: NPC, displayName: String, vararg options: Pair<Int, String>) {
        write(player, npc, 0, displayName, options)
    }

    fun clear(player: Player, npc: NPC) {
        write(player, npc, 1, "", emptyArray())
    }

    private fun write(
        player: Player,
        npc: NPC,
        flags: Int,
        displayName: String,
        options: Array<out Pair<Int, String>>
    ) {
        val session = player.session ?: return
        val buf = IoBuffer(OPCODE, PacketHeader.BYTE)
        buf.p2(npc.index)
        buf.p2(npc.id)
        buf.p1(flags)
        buf.putString(displayName)
        buf.p1(options.size)
        for ((slot, text) in options) {
            buf.p1(slot)
            buf.putString(text)
        }
        buf.cypherOpcode(session.isaacPair.output)
        session.write(buf)
    }
}