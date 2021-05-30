import { Stack } from '../../core/Structures';
import FreeCell from '../entities/FreeCell';

export class Field {
    constructor() {
        this.foundations = [new Stack(), new Stack(), new Stack()] //TODO: Test case
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