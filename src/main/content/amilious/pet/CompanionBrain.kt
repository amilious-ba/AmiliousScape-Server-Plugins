package content.amilious.pet

import content.amilious.pet.actions.CompanionAction

class CompanionBrain<T>(private val actor: T) {

    fun currentName(): String = current?.name() ?: "idle"

    private val actions = ArrayList<CompanionAction<T>>()
    private var current: CompanionAction<T>? = null

    fun addAction(action: CompanionAction<T>): CompanionBrain<T> {
        actions.add(action)
        return this
    }

    fun tick() {
        actions.forEach { it.cooldown(actor) }
        val running = current
        if (running != null && running.tick(actor)) return
        current = actions.firstOrNull { it.canStart(actor) }
        current?.start(actor)
        val next = current ?: return
        if (!next.tick(actor)) current = null
    }

    fun interrupt() {
        current = null
    }
}