import DealerBase from "../../core/gameutils/DealerBase.js"

export default class Dealer extends DealerBase {
    constructor() {
        super();
    }

    deal(playField) {
        this.deck.forEach((card, index) => {
            const stackNumber = index % 8;
            
            playField.columns[stackNumber].push(card);
        });
    }
}