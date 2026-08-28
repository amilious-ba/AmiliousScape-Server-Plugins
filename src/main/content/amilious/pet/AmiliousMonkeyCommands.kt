package content.amilious.pet

import core.game.node.entity.player.Player
import core.game.system.command.Privilege
import core.game.system.command.sets.CommandSet
import core.game.world.repository.Repository
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyCommands : CommandSet(Privilege.STANDARD) {

    override fun defineCommands() {
        define("monkey", Privilege.STANDARD, "::monkey", "Summon or dismiss your monkey.") { player, _ ->
            if (!owns(player)) {
                reject(player, "This monkey only answers to its owner.")
            }
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE)
            if (live != null) {
                live.dismiss()
            } else {
                AmiliousMonkey(player).spawnAtOwner()
            }
        }

        define("monkeybag", Privilege.STANDARD, "::monkeybag", "List what the monkey is carrying.") { player, _ ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE)
                ?: reject(player, "Your monkey is not out.")
            notify(player, "Monkey pack:")
            val items = live.bag.toArray().filterNotNull()
            if (items.isEmpty()) {
                notify(player, " (empty)")
            } else {
                items.forEach { notify(player, " - ${it.amount} x ${it.name}") }
            }
        }

        define("grantmonkey", Privilege.ADMIN, "::grantmonkey player_name", "Give the companion monkey.") { player, args ->
            if (args.size < 2) {
                reject(player, "Usage: ::grantmonkey player_name")
            }
            val name = args.drop(1).joinToString("_")
            val target = Repository.getPlayerByName(name)
                ?: reject(player, "Player not online: $name")
            target.setAttribute(MonkeyConfig.ATTR_OWNED, true)
            notify(player, "Granted monkey to ${target.username}.")
            notify(target, "A monkey has taken a liking to you. Type ::monkey")
        }
    }

    private fun owns(player: Player): Boolean {
        if (player.getAttribute(MonkeyConfig.ATTR_OWNED, false)) return true
        return player.name.lowercase() in MonkeyConfig.OWNERS
    }
}