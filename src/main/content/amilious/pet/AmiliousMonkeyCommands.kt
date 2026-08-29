package content.amilious.pet

import core.api.setAttribute
import core.game.node.entity.player.Player
import core.game.system.command.Privilege
import core.game.system.command.sets.CommandSet
import core.game.world.repository.Repository
import core.plugin.Initializable

@Initializable
class AmiliousMonkeyCommands : CommandSet(Privilege.STANDARD) {

    override fun defineCommands() {
        define("monkey", Privilege.STANDARD, "::monkey", "Summon or dismiss Gigos.") { player, _ ->
            if (!owns(player)) {
                reject(player, "Gigos only answers to its owner.")
            }
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live != null) {
                live.dismiss()
            } else {
                AmiliousMonkey(player).spawnAtOwner()
            }
        }

        define("monkeybag", Privilege.STANDARD, "::monkeybag", "Open Gigos' pack.") { player, _ ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null) {
                reject(player, "Gigos is not out.")
            }
            live!!.openBagUi()
        }

        define("monkeytake", Privilege.STANDARD, "::monkeytake", "Take everything from Gigos.") { player, _ ->
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null) {
                reject(player, "Gigos is not out.")
            }
            var moved = 0
            for (item in live!!.bag.toArray()) {
                if (item == null) continue
                if (player.inventory.add(item)) {
                    live.bag.remove(item)
                    moved++
                } else {
                    notify(player, "Inventory full. Left the rest with Gigos.")
                    break
                }
            }
            live.saveBag()
            notify(player, if (moved == 0) "Gigos is not carrying anything." else "You take Gigos' pack.")
        }

        define("gigosop", Privilege.STANDARD, "::gigosop slot", "Monkey menu actions.") { player, args ->
            if (args.size < 2) {
                reject(player, "Invalid monkey action.")
            }
            val slot = args[1].toIntOrNull()
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            when (slot) {
                1 -> notify(player, "Gigos chatters and looks at you.")
                0 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    live!!.openBagUi()
                }
                2 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    val on = player.getAttribute(MonkeyConfig.ATTR_LOOT, true)
                    setAttribute(player, MonkeyConfig.ATTR_LOOT, !on)
                    live!!.refreshMenu()
                    notify(player, if (!on) "Gigos will loot your kills." else "Gigos will not loot.")
                }
                3 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    live!!.dismiss()
                }
                4 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    val on = player.getAttribute(MonkeyConfig.ATTR_DUNG, true)
                    setAttribute(player, MonkeyConfig.ATTR_DUNG, !on)
                    GigosHudPacket.send(player, live!!)
                    notify(player, if (!on) "Gigos will throw dung." else "Gigos will not throw dung.")
                }
                else -> reject(player, "Unknown monkey action.")
            }
        }

        define(
            "grantmonkey",
            Privilege.ADMIN,
            "::grantmonkey player_name",
            "Give the companion monkey."
        ) { player, args ->
            if (args.size < 2) {
                reject(player, "Usage: ::grantmonkey player_name")
            }
            val name = args.drop(1).joinToString("_")
            val target = Repository.getPlayerByName(name)
            if (target == null) {
                reject(player, "Player not online: $name")
            }
            setAttribute(target!!, MonkeyConfig.ATTR_OWNED, true)
            notify(player, "Granted monkey to ${target.username}.")
            notify(target, "Gigos has taken a liking to you. Type ::monkey")
        }
    }

    private fun owns(player: Player): Boolean {
        if (player.getAttribute(MonkeyConfig.ATTR_OWNED, false)) return true
        return player.name.lowercase() in MonkeyConfig.OWNERS
    }
}