import CardActor from '../core/Actors/CardActor';
import { Field } from './gameutils/Field';
import FreecellDealer from './gameutils/FreecellDealer';

export default class FreecellGame {
    constructor(engine) {
        this.engine = engine;
        this.dealer = new FreecellDealer()
    }

    startNewGame() {
        const newGameField = new Field();
        this.dealer.deal(newGameField);
        this.runGame(newGameField)
    }

    setUpActors(field) {
        const me = this;
        //This should be thrown into a scene sometime.
        this.actors = field.tableau.map(column => column.peekAll()).flat().map(card => new CardActor(me.engine, card));
    }

    runGame(field) {
        this.setUpActors(field);
        this.actors.forEach(a => this.engine.add(a), this);
    }
}