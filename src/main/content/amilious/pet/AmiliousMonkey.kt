package content.amilious.pet

import core.api.sendMessage
import core.game.container.Container
import core.game.interaction.MovementPulse
import core.game.node.entity.combat.ImpactHandler
import core.game.node.entity.npc.NPC
import core.game.node.entity.player.Player
import core.game.node.item.GroundItem
import core.game.node.item.GroundItemManager
import core.game.node.item.Item
import core.game.world.map.RegionManager
import core.game.world.update.flag.context.Graphics

class AmiliousMonkey(val owner: Player) : NPC(MonkeyConfig.NPC_ID) {

    val bag = Container(MonkeyConfig.BOB_SIZE)
    private var dungWait = 0

    fun spawnAtOwner() {
        location = owner.location.transform(1, 0, 0)
        init()
        isWalks = true
        owner.setAttribute(MonkeyConfig.ATTR_ACTIVE, this)
        sendMessage(owner, "Your monkey hops down beside you.")
        followOwner()
    }

    fun dismiss() {
        for (item in bag.toArray()) {
            if (item != null) {
                GroundItemManager.create(item, location, owner)
            }
        }
        bag.clear()
        owner.removeAttribute(MonkeyConfig.ATTR_ACTIVE)
        clear()
        sendMessage(owner, "Your monkey scurries off. ::monkey to call him back.")
    }

    fun tickCompanion() {
        if (!owner.isActive) {
            dismiss()
            return
        }
        if (dungWait > 0) dungWait--

        if (location.getDistance(owner.location) > MonkeyConfig.FOLLOW_DIST) {
            properties.teleportLocation = owner.location
        }

        tryDung()
        if (!pulseManager.hasPulseRunning()) {
            followOwner()
        }
    }

    private fun followOwner() {
        pulseManager.run(object : MovementPulse(this, owner) {
            override fun pulse(): Boolean = false
        })
    }

    private fun tryDung() {
        if (dungWait > 0) return
        val victim = owner.properties.combatPulse?.victim ?: return
        if (victim === owner || victim === this) return
        if (location.getDistance(victim.location) > 8) return

        dungWait = MonkeyConfig.DUNG_COOLDOWN
        face(victim)
        victim.graphics(Graphics(30))
        val hit = 1 + (Math.random() * MonkeyConfig.DUNG_MAX_HIT).toInt()
        victim.impactHandler.manualHit(this, hit, ImpactHandler.HitsplatType.NORMAL)
        victim.properties.combatPulse.attack(this)
        sendMessage(owner, "Your monkey flings something foul. ${victim.name} looks furious.")
    }
}