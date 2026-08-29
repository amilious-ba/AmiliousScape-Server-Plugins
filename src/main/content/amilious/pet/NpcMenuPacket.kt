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

        val payload = IoBuffer()
        payload.p2(npc.index)
        payload.p2(npc.id)
        payload.p1(flags)
        payload.putString(displayName)
        payload.p1(options.size)
        for ((slot, text) in options) {
            payload.p1(slot)
            payload.putString(text)
        }
        val n = payload.toByteBuffer().position()

        val buf = IoBuffer(OPCODE, PacketHeader.NORMAL)
        buf.p1(n)
        payload.toByteBuffer().flip()
        buf.put(payload.toByteBuffer())
        buf.cypherOpcode(session.isaacPair.output)
        session.write(buf)
    }
}