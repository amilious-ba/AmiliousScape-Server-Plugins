package content.amilious.commands

import core.api.playAudio
import core.game.node.entity.player.info.Rights
import core.game.system.command.Privilege
import core.game.system.command.sets.CommandSet
import core.game.world.repository.Repository
import core.plugin.Initializable
import core.game.node.entity.skill.Skills

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

        define(
            "setmod",
            Privilege.ADMIN,
            "::setmod player_name",
            "Give moderator rights to an online player."
        ) { player, args ->
            if (args.size < 2) {
                reject(player, "Usage: ::setmod player_name")
            }
            val name = args.drop(1).joinToString("_")
            val target = Repository.getPlayerByName(name)
            if (target == null) {
                reject(player, "Player not online: $name")
            }
            target!!.details.rights = Rights.PLAYER_MODERATOR
            notify(player, "Gave moderator to ${target.username}.")
            notify(target, "You are now a moderator.")
        }

        define(
            "unmod",
            Privilege.ADMIN,
            "::unmod player_name",
            "Remove moderator rights from an online player."
        ) { player, args ->
            if (args.size < 2) {
                reject(player, "Usage: ::unmod player_name")
            }
            val name = args.drop(1).joinToString("_")
            val target = Repository.getPlayerByName(name)
            if (target == null) {
                reject(player, "Player not online: $name")
            }
            if (target!!.details.rights == Rights.ADMINISTRATOR) {
                reject(player, "Refusing to demote an admin. Use ::dropadmin on that account.")
            }
            target.details.rights = Rights.REGULAR_PLAYER
            notify(player, "Removed moderator from ${target.username}.")
            notify(target, "Your moderator rank was removed.")
        }

        define(
            "setxp",
            Privilege.ADMIN,
            "::setxp rate [player_name]",
            "Set a player's XP multiplier (default world rate is in config)."
        ) { player, args ->
            if (args.size < 2) {
                reject(player, "Usage: ::setxp rate [player_name]")
            }
            val rate = args[1].toDoubleOrNull()
            if (rate == null || rate <= 0.0 || rate > 50.0) {
                reject(player, "Rate must be a number from 0.1 to 50. Example: ::setxp 5")
            }
            val target = if (args.size >= 3) {
                val name = args.drop(2).joinToString("_")
                val found = Repository.getPlayerByName(name)
                if (found == null) {
                    reject(player, "Player not online: $name")
                }
                found!!
            } else {
                player
            }
            target.skills.experienceMultiplier = rate!!
            notify(player, "Set ${target.username} XP rate to ${rate}x.")
            if (target != player) {
                notify(target, "Your XP rate is now ${rate}x.")
            }
        }

        define("gsfx", Privilege.ADMIN, "::gsfx id", "Play a sound") { player, args ->
            playAudio(player, args[1].toInt())
        }

        define(
            "findamilious",
            Privilege.STANDARD,
            "::findamilious",
            "Wolfy only. Teleport to Amilious."
        ) { player, _ ->
            if (!player.name.equals("Wolfy", ignoreCase = true)) {
                reject(player, "This command is not for you.")
            }
            val dest = Repository.getPlayerByName("Amilious")
            if (dest == null) {
                reject(player, "Amilious is not online.")
            }
            player.properties.teleportLocation = dest!!.location
            notify(player, "You run to Amilious.")
        }

        define(
            "levels",
            Privilege.ADMIN,
            "::levels [player_name]",
            "Print an online player's skill levels."
        ) { player, args ->
            val target = if (args.size >= 2) {
                val name = args.drop(1).joinToString("_")
                val found = Repository.getPlayerByName(name)
                if (found == null) {
                    reject(player, "Player not online: $name")
                }
                found!!
            } else {
                player
            }
            notify(player, "${target.username}  combat ${target.properties.currentCombatLevel}  total ${target.skills.totalLevel}")
            for (i in 0 until Skills.SKILL_NAME.size) {
                notify(player, "${Skills.SKILL_NAME[i]}  ${target.skills.getStaticLevel(i)}")
            }
        }

    }
}