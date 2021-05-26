import { Stack } from '../../core/Structures';

export class Field {
    constructor() {
        this.freeCells = [null, null, null, null];
        this.columns = [
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