import { Actor, Color, Vector } from 'excalibur';

export default class CardActor extends Actor {
    constructor(card) {
        super();
        this.card = card;
        card.actors = [this];

        this.initializeVisualConfig();
    }

    initializeVisualConfig() {
        this.color = Color.Chartreuse;
    }

    onInitialize(engine) {
        const cardBackSprite = this.resources.cardBack.asSprite();
        cardBackSprite.scale = new Vector(this.width / cardBackSprite.width, this.height / cardBackSprite.height);
        this.addDrawing('back', cardBackSprite);
    }
}