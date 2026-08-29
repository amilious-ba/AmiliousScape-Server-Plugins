package content.amilious.pet

import core.api.sendMessage
import core.game.node.item.Item
import core.game.container.Container
import core.game.node.entity.npc.NPC
import core.game.node.item.GroundItem
import core.game.world.map.RegionManager
import core.game.interaction.MovementPulse
import core.game.node.entity.player.Player
import core.game.node.item.GroundItemManager
import core.game.node.entity.combat.ImpactHandler
import core.game.world.update.flag.context.Graphics

import core.game.component.CloseEvent
import core.game.component.Component
import core.game.container.access.InterfaceContainer

class AmiliousMonkey(val owner: Player) : NPC(MonkeyConfig.NPC_ID) {

    val bag = Container(MonkeyConfig.BOB_SIZE)
    private var dungWait = 0
    private var lootBusy = false

    fun spawnAtOwner() {
        location = owner.location.transform(1, 0, 0)
        init()
        loadBag()
        name = "Gigos"
        isWalks = true
        owner.setAttribute(MonkeyConfig.ATTR_ACTIVE, this)
        sendMessage(owner, "Gigos hops down beside you.")
        followOwner()
    }

    fun dismiss() {
        saveBag()
        bag.clear()
        owner.removeAttribute(MonkeyConfig.ATTR_ACTIVE)
        clear()
        sendMessage(owner, "Gigos scurries off. ::monkey to call him back.")
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
        tryLoot()
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

    fun saveBag() {
        val encoded = bag.toArray()
            .filterNotNull()
            .joinToString(";") { "${it.id}:${it.amount}" }
        owner.setAttribute(MonkeyConfig.ATTR_BAG, encoded)
    }

    fun loadBag() {
        bag.clear()
        val raw = owner.getAttribute(MonkeyConfig.ATTR_BAG, "") ?: return
        if (raw.isBlank()) return
        for (part in raw.split(";")) {
            val bits = part.split(":")
            if (bits.size != 2) continue
            val id = bits[0].toIntOrNull() ?: continue
            val amt = bits[1].toIntOrNull() ?: continue
            if (id > 0 && amt > 0) bag.add(Item(id, amt))
        }
    }

    fun openBagUi() {
        owner.interfaceManager.open(Component(671)).setCloseEvent(CloseEvent { player, _ ->
            saveBag()
            player.interfaceManager.closeSingleTab()
            true
        })
        bag.shift()
        owner.interfaceManager.openSingleTab(Component(665))
        InterfaceContainer.generateItems(
            owner, owner.inventory.toArray(),
            arrayOf("Examine", "Store-X", "Store-All", "Store-10", "Store-5", "Store-1"),
            665, 0, 7, 4, 93
        )
        InterfaceContainer.generateItems(
            owner, bag.toArray(),
            arrayOf("Examine", "Withdraw-X", "Withdraw-All", "Withdraw-10", "Withdraw-5", "Withdraw-1"),
            671, 27, 5, 6, 30
        )
    }

    private fun tryDung() {
        if (dungWait > 0) return
        if (!owner.properties.combatPulse.isAttacking) return

        val victim = RegionManager.getLocalNpcs(owner, 8)
            .firstOrNull {
                it !== this &&
                        it.isActive &&
                        !it.isInvisible &&
                        it.properties.combatPulse.isAttacking
            } ?: return

        if (victim === owner || victim === this) return
        if (location.getDistance(victim.location) > 8) return

        dungWait = MonkeyConfig.DUNG_COOLDOWN
        face(victim)
        victim.graphics(Graphics(30))
        val hit = 1 + (Math.random() * MonkeyConfig.DUNG_MAX_HIT).toInt()
        victim.impactHandler.manualHit(this, hit, ImpactHandler.HitsplatType.NORMAL)
        victim.properties.combatPulse.attack(this)
        sendMessage(owner, "Gigos flings something foul. ${victim.name} looks furious.")
    }

    private fun tryLoot() {
        if (lootBusy) return
        if (bag.freeSlots() <= 0) return

        val nearby = GroundItemManager.getItems()
            .filter { gi ->
                gi != null &&
                        !gi.isRemoved &&
                        gi.location.getDistance(location) <= MonkeyConfig.LOOT_RANGE &&
                        bag.hasSpaceFor(gi)
            }

        val target: GroundItem = nearby.minByOrNull { gi ->
            gi.location.getDistance(location)
        } ?: return

        lootBusy = true
        pulseManager.run(object : MovementPulse(this, target) {
            override fun pulse(): Boolean {
                lootBusy = false
                if (target.isRemoved) return true
                val copy = Item(target.id, target.amount)
                if (bag.add(copy)) {
                    GroundItemManager.destroy(target)
                    sendMessage(owner, "Gigos scoops up the ${copy.name.lowercase()}.")
                }
                return true
            }
        })
    }

}