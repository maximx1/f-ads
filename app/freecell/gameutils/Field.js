import { Stack } from '../../core/Structures';

export class Field {
    constructor() {
        this.foundations = [new Stack(), new Stack(), new Stack()] //TODO: Test case
        this.freeCells = [null, null, null, null];
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