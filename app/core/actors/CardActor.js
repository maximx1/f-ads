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
        // TODO: This should be handled by something outside this.
        const playAreaWidth = Math.min(700, this.engine.drawWidth),
            gutterSpace = playAreaWidth / 49,
            cardWidth = 5 * playAreaWidth / 49,
            cardHeight = 6 * cardWidth / 5;

        this.pos.x = 10;
        this.pos.y = 10;//this.engine.drawHeight - 40;
        this.width = cardWidth;
        this.height = cardHeight;
        this.color = Color.Chartreuse
    }
}