package content.amilious.pet

//
import core.net.packet.IoBuffer
import core.net.packet.PacketHeader
import core.game.node.entity.player.Player


/**
 * GigosHudPacket is responsible for constructing and sending game-related packets
 * to update player states, hide players, or handle AmiliousMonkey configurations.
 * This object provides functionality to build and transmit the appropriate
 * IoBuffer packets for client-server communication.
 */
object GigosHudPacket {

    //#region Constants ################################################################################################

    /**
     * Represents the opcode constant used for a specific operation or instruction in the protocol.
     * Typically utilized for client-server communication to identify and handle particular actions.
     *
     * Value: 201
     */
    const val OPCODE = 201

    //#endregion #######################################################################################################


    //#region Public Methods ###########################################################################################

    /**
     * Sends a packet to hide a player in the session.
     *
     * This method constructs a packet with a specific opcode and header, then writes it
     * to the player's session. The packet is used to indicate that the player should be
     * hidden in the game world.
     *
     * @param player The player that should be hidden. The session associated with this player
     * is used to send the packet.
     */
    fun hide(player: Player) {
        val buf = IoBuffer(OPCODE, PacketHeader.BYTE)
        buf.p1(0)
        player.session.write(buf)
    }

    /**
     * Sends a packet to the player's session, updating the state of an AmiliousMonkey with its configurations
     * and current attributes.
     *
     * @param player The player to whom the packet should be sent.
     * @param monkey The AmiliousMonkey whose data and configurations will be sent in the packet.
     */
    fun send(player: Player, monkey: AmiliousMonkey) {
        val lootOn = player.getAttribute(MonkeyConfig.ATTR_LOOT, true)
        val dungOn = player.getAttribute(MonkeyConfig.ATTR_DUNG, true)
        val eatOn = player.getAttribute(MonkeyConfig.ATTR_EAT, true)
        val b2bOn = player.getAttribute(MonkeyConfig.ATTR_B2B, true)
        val feedOn = player.getAttribute(MonkeyConfig.ATTR_FEED, true)
        val pickOn = player.getAttribute(MonkeyConfig.ATTR_PICK, true)
        val unburdenOn = player.getAttribute(MonkeyConfig.ATTR_UNBURDEN, true)
        val darkOn = player.getAttribute(MonkeyConfig.ATTR_DARK, true)
        val graveOn = player.getAttribute(MonkeyConfig.ATTR_GRAVE, true)

        val buf = IoBuffer(OPCODE, PacketHeader.BYTE)
        buf.p1(2)
        buf.p2(monkey.hunger().coerceIn(0, 65535))
        buf.p2(MonkeyConfig.HUNGER_MAX)
        buf.putString("Gigos")
        buf.p2(monkey.bananaCount().coerceIn(0, 65535))
        buf.putString(monkey.brainActionName())
        buf.putString(monkey.brainPhaseName())
        buf.p1(monkey.brainPhase().coerceIn(0, 255))
        buf.p1(monkey.brainPhases().coerceIn(0, 255))
        buf.p1(if (monkey.brainBusy()) 1 else 0)
        buf.p1(if (monkey.isDrunk()) 1 else 0)
        buf.p2(monkey.drunkTicks().coerceIn(0, 65535))
        buf.p1(9)
        buf.p1(10)
        buf.p1(if (darkOn) 1 else 0)
        buf.putString("Dark")
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
        buf.p1(8)
        buf.p1(if (pickOn) 1 else 0)
        buf.putString("Forage")
        buf.p1(9)
        buf.p1(if (unburdenOn) 1 else 0)
        buf.putString("Unburden")
        buf.p1(11) // client button id — match ::gigosop
        buf.p1(if (graveOn) 1 else 0)
        buf.putString("Loot-Grave")
        player.session.write(buf)
    }

    //#endregion Public Methods ########################################################################################

}