import { Stack } from "../Structures";

export default class Foundation {
    constructor() {
        this.pile = new Stack();
    }

    getActiveActors() {
        return this.pile.isEmpty() ? [this.emptyCardSlotActor] : this.pile.peekAll();
    }
}