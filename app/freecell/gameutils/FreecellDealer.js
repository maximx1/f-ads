import DealerBase from "../../core/gameutils/DealerBase.js"

export default class FreecellDealer extends DealerBase {
    constructor() {
        super();
    }

    deal(playField) {
        this.deck.forEach((card, index) => {
            const stackNumber = index % 8;
            
            playField.tableau[stackNumber].push(card);
        });
    }
}