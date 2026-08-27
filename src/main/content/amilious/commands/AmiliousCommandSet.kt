package content.amilious.commands

import core.api.notify
import core.game.system.command.Privilege
import core.game.system.command.sets.CommandSet
import core.plugin.Initializable

/**
 * Starter Amilious command set. Add more define() blocks here.
 * Overlay this file into Server/src/main/content/amilious/commands/
 * then rebuild the server.
 */
@Initializable
class AmiliousCommandSet : CommandSet(Privilege.ADMIN) {

    override fun defineCommands() {
        define(
            "amilious",
            Privilege.STANDARD,
            "::amilious",
            "Confirms Amilious server plugins are loaded."
        ) { player, _ ->
            notify(player, "Amilious server plugins are loaded.")
        }
    }
}