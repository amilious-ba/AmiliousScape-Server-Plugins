package content.amilious.pet.actions

interface CompanionAction<T> {
    fun name(): String
    fun canStart(actor: T): Boolean
    fun start(actor: T) {}
    fun tick(actor: T): Boolean
    fun cooldown(actor: T) {}
}