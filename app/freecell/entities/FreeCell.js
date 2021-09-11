export default class FreeCell {
    constructor() {
        this.card = null;
        this.emptyCardSlotActor = null;
    }

    placeCard(card) {
        if(this.isFree()) {
            this.card = card;
            return true;
        } else {
            return false;
        }
    }

    pickUpCard() {
        const cardToReturn =  this.card;
        this.card = null;
        return cardToReturn;
    }

    isFree() {
        return this.card == null;
    }

    getActiveActors() {
        return this.isFree() ? [this.emptyCardSlotActor] : this.card.actors;
    }
}