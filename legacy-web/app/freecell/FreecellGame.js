import CardActor from '../core/actors/CardActor';
import { Field } from './gameutils/Field';
import FreecellDealer from './gameutils/FreecellDealer';
import FreecellChoreographer from './gameutils/FreecellChoreographer';
import EmptyCardSlotActor from '../core/actors/EmptyCardSlotActor';

export default class FreecellGame {
    constructor(engine, resources) {
        this.engine = engine;
        this.resources = resources;
        this.dealer = new FreecellDealer();
    }

    startNewGame() {
        const newGameField = new Field();
        this.dealer.deal(newGameField);
        this.runGame(newGameField)
    }

    castActors(field) {
        //This should be thrown into a scene sometime.
        this.actors = field.tableau.map(column => column.peekAll()).flat().map(card => new CardActor(card), this);
        this.actors.push(...field.freeCells.map(freeCell => new EmptyCardSlotActor(freeCell)));
        this.actors.push(...field.foundations.map(foundation => new EmptyCardSlotActor(foundation)));
    }

    runGame(field) {
        const choreographer = new FreecellChoreographer(this.engine);
        choreographer.resources = this.resources;
        choreographer.scalePlayArea();
        
        this.castActors(field);
        choreographer.updateActors(field);
        this.actors.forEach(a => this.engine.add(a), this);
    }
}