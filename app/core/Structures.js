export class Stack {
    constructor(...initialList) {
        this.items = [...initialList.filter((i) => i != null)];
    }

    push(...newItems) {
        this.items.push(...newItems.filter((i) => i != null));
    }

    pop() {
        if(this.isEmpty()) return null;
        return this.items.pop();
    }

    take(n) {
        if(this.size() < n) return null;
        return this.items.splice(-1 * n);
    }

    peek() {
        if(this.isEmpty()) return null;
        return this.items.slice(-1)[0];
    }

    peekMany(n) {
        if(this.size() < n) return null;
        return this.items.slice(-1 * n);
    }

    peekAll() {
        return this.items.slice();
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