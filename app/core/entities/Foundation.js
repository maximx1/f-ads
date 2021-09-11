import { Stack } from "../Structures";

export default class Foundation {
    constructor() {
        this.pile = new Stack();
    }

    // TODO: Add and remove cards from pile.

    getActiveActors() {
        // TODO: I think this pile.peekAll() might need to map to actors for each card down the road
        return this.pile.isEmpty() ? [this.emptyCardSlotActor] : this.pile.peekAll();
    }
}