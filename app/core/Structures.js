export class Stack {
    constructor(...initialList) {
        this.items = initialList != null ? [...initialList.filter((i) => i != null)] : [];
    }

    push(...newItems) {
        this.items.push(...newItems.filter((i) => i != null));
    }

    pop() {
        if(this.isEmpty()) return null;
        return this.items.pop();
    }

    peek() {
        if(this.isEmpty()) return null;
        return this.items.slice(-1)[0];
    }

    isEmpty() {
        return this.items.length === 0;
    }

    clone() {
        return new Stack(...this.items);
    }

    size() {
        return this.items.length;
    }
}