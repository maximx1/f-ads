import CardActor from '../core/actors/CardActor';
import { Field } from './gameutils/Field';
import FreecellDealer from './gameutils/FreecellDealer';
import FreecellChoreographer from './gameutils/FreecellChoreographer';

export default class FreecellGame {
    constructor(engine) {
        this.engine = engine;
        this.dealer = new FreecellDealer();
    }

    startNewGame() {
        const newGameField = new Field();
        this.dealer.deal(newGameField);
        this.runGame(newGameField)
    }

    castActors(field) {
        //This should be thrown into a scene sometime.
        this.actors = field.tableau.map(column => column.peekAll()).flat().map(card => new CardActor(this.engine, card), this);
    }

    runGame(field) {
        const choreographer = new FreecellChoreographer(this.engine);
        choreographer.scalePlayArea();
        
        this.castActors(field);
        choreographer.updateActors(field);

        this.actors.forEach(a => this.engine.add(a), this);
    }
}