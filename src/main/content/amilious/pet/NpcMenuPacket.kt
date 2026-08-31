package content.amilious.pet


import core.net.packet.IoBuffer
import core.net.packet.PacketHeader
import core.game.node.entity.npc.NPC
import core.game.node.entity.player.Player


/**
 * The `NpcMenuPacket` object is responsible for handling communication related to NPC menus between the client
 * and server. It provides functionality to send NPC interaction data as packets and manage associated states.
 */
object NpcMenuPacket {

    //#region Constants ################################################################################################

    /**
     * Represents the opcode constant used for communication between the client and server.
     * This value is typically referenced in protocol handling to identify a specific
     * operation or instruction.
     *
     * Value: 200
     */
    const val OPCODE = 200

    //#endregion #######################################################################################################


    //#region Public Methods ###########################################################################################

    /**
     * Sends a packet to the client representing an interaction or display of NPC-related options.
     *
     * @param player The player to send the packet to.
     * @param npc The NPC associated with the packet.
     * @param displayName The display name of the NPC to present to the player.
     * @param options A vararg array of option pairs where each pair consists of a slot (Int) and a corresponding text (String).
     */
    fun send(player: Player, npc: NPC, displayName: String, vararg options: Pair<Int, String>) {
        write(player, npc, 0, displayName, options)
    }

    /**
     * Clears the current state or configuration related to the given player and NPC.
     *
     * @param player The player whose state or interaction with the NPC is to be cleared.
     * @param npc The NPC whose state or interaction with the player is to be cleared.
     */
    fun clear(player: Player, npc: NPC) {
        write(player, npc, 1, "", emptyArray())
    }

    //#endregion #######################################################################################################


    //#region Private Methods ##########################################################################################

    /**
     * Writes a packet to the player's session containing information about an NPC along with additional options.
     *
     * @param player The player for whom the packet is being written.
     * @param npc The NPC associated with the packet.
     * @param flags Additional flags that provide metadata about the NPC.
     * @param displayName The display name of the NPC to be displayed to the player.
     * @param options An array of option pairs where each pair consists of a slot (Int) and a corresponding text (String).
     */
    private fun write(player: Player, npc: NPC, flags: Int, displayName: String,
                      options: Array<out Pair<Int, String>>) {
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
        buf.packetSize = buf.toByteBuffer().position()
        buf.cypherOpcode(session.isaacPair.output)
        session.write(buf)
    }

    //#endregion Private Methods #######################################################################################

}