export default class FreeCell {
    constructor() {
        this.card = null;
        this.emptyCardSlotActor = null;
    }

    isFree() {
        return this.card == null;
    }

    getActiveActors() {
        return this.isFree() ? [this.emptyCardSlotActor] : this.card.actors;
    }
}