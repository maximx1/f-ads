import { Actor, Color } from 'excalibur';

export default class CardActor extends Actor {
    constructor(engine, card) {
        super();
        this.engine = engine;
        this.card = card;
        card.actors = [this];

        this.initializeVisualConfig();
    }

    initializeVisualConfig() {
        this.color = Color.Chartreuse;
    }
}