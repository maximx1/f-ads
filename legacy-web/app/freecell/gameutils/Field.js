import Foundation from '../../core/entities/Foundation';
import { Stack } from '../../core/Structures';
import FreeCell from '../entities/FreeCell';

export class Field {
    constructor() {
        this.foundations = [new Foundation(), new Foundation(), new Foundation(), new Foundation()]
        this.freeCells = [new FreeCell(), new FreeCell(), new FreeCell(), new FreeCell()];
        this.tableau = [
            new Stack(),
            new Stack(),
            new Stack(),
            new Stack(),
            new Stack(),
            new Stack(),
            new Stack(),
            new Stack()
        ]
    }
}