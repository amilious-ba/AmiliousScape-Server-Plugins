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

        define(
            "gigosani",
            Privilege.STANDARD,
            "::gigosani id",
            "Play an animation on Gigos."
        ) { player, args ->
            if (args.size < 2) {
                reject(player, "Usage: ::gigosani id")
            }
            val id = args[1].toIntOrNull()
            if (id == null || id < 0) {
                reject(player, "Usage: ::gigosani id")
            }
            val live = player.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)
            if (live == null) {
                reject(player, "Gigos is not out.")
            }
            live!!.animate(core.game.world.update.flag.context.Animation(id!!))
            notify(player, "Gigos animation $id")
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
                5 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    val on = player.getAttribute(MonkeyConfig.ATTR_EAT, true)
                    setAttribute(player, MonkeyConfig.ATTR_EAT, !on)
                    GigosHudPacket.send(player, live!!)
                    notify(player, if (!on) "Gigos will eat bananas." else "Gigos will not eat.")
                }
                6 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    val on = player.getAttribute(MonkeyConfig.ATTR_B2B, true)
                    setAttribute(player, MonkeyConfig.ATTR_B2B, !on)
                    GigosHudPacket.send(player, live!!)
                    notify(player, if (!on) "Gigos will turn bones into bananas." else "Gigos will not convert bones.")
                }
                7 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    val on = player.getAttribute(MonkeyConfig.ATTR_FEED, true)
                    setAttribute(player, MonkeyConfig.ATTR_FEED, !on)
                    GigosHudPacket.send(player, live!!)
                    notify(player, if (!on) "Gigos will feed you." else "Gigos will not feed you.")
                }
                8 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    val on = player.getAttribute(MonkeyConfig.ATTR_PICK, true)
                    setAttribute(player, MonkeyConfig.ATTR_PICK, !on)
                    GigosHudPacket.send(player, live!!)
                    notify(player, if (!on) "Gigos will pick bananas." else "Gigos will not pick bananas.")
                }
                9 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    val on = player.getAttribute(MonkeyConfig.ATTR_UNBURDEN, true)
                    setAttribute(player, MonkeyConfig.ATTR_UNBURDEN, !on)
                    GigosHudPacket.send(player, live!!)
                    notify(player, if (!on) "Gigos will unburden you." else "Gigos will not unburden you.")
                }
                10 -> {
                    if (live == null) reject(player, "Gigos is not out.")
                    val on = player.getAttribute(MonkeyConfig.ATTR_DARK, true)
                    setAttribute(player, MonkeyConfig.ATTR_DARK, !on)
                    live!!.applyModel()
                    GigosHudPacket.send(player, live)
                    notify(player, if (!on) "Gigos is dark." else "Gigos is light.")
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