import { expect } from 'chai';
import { Stack } from '../../app/core/Structures.js';

describe('Stack', () => {
    describe('initialization', () => {
        it('should be able to be initialized empty', () => {
            const stack = new Stack();
            expect(stack.isEmpty()).to.be.true;
        });

        it('should be able to be initialized with a list', () => {
            const stack = new Stack(1, 2, 3, 4, 5);
            expect(stack.items).to.eql([1, 2, 3, 4, 5]);
        });

        it('should not be allowed to initialize with null or undefined values', () => {
            const stack1 = new Stack(null);
            const stack2 = new Stack(undefined);
            const stack3 = new Stack(1, null, undefined);
    
            expect(stack1.items).to.be.empty;
            expect(stack2.items).to.be.empty;
            expect(stack3.items).to.eql([1]);
        });
    });

    describe('#push', () => {
        it('should be able to push items into stack', () => {
            const stack = new Stack();
            expect(stack.items).to.be.empty;
    
            stack.push(1);
            expect(stack.items).to.eql([1]);
    
            stack.push(2);
            expect(stack.items).to.eql([1, 2]);
    
            stack.push(3);
            expect(stack.items).to.eql([1, 2, 3]);
        });
    
        it('should not be allowed to push null or undefined into the stack', () => {
            const stack = new Stack();
            stack.push(null);
            stack.push();
            stack.push(null, undefined);
            expect(stack.items).to.be.empty;
    
            stack.push(1, null, 2, undefined);
            expect(stack.items).to.eql([1, 2]);
        });

        it('should be able to push multiple items into stack at once', () => {
            const stack = new Stack();
            stack.push(1, 2, 3);
            expect(stack.items).to.eql([1, 2, 3]);
        })
    });

    describe('#pop', () => {
        it('should be able to pop items from stack', () => {
            const stack = new Stack(1,2,3);
            
            expect(stack.pop()).to.equal(3);
            expect(stack.items).to.eql([1, 2]);
    
            expect(stack.pop()).to.equal(2);
            expect(stack.items).to.eql([1]);
    
            expect(stack.pop()).to.equal(1);
            expect(stack.items).to.be.empty;
        });
    
        it('should return null if stack is empty', () => {
            const stack = new Stack();
            expect(stack.pop()).to.be.null;
        });
    });

    describe('#take', () => {
        it('should return n items from the top of the stack as a list', () => {
            const stack = new Stack(1, 2, 3, 4);
            expect(stack.take(2)).to.eql([3, 4]);
        });

        it('should reduce the size of the stack', () => {
            const stack = new Stack(1, 2, 3, 4),
                taken = stack.take(2);

            expect(stack.items).to.eql([1, 2]);
            expect(taken).to.eql([3, 4]);
        });

        it('should return null when trying to take more than the stack has in it', () => {
            const stack = new Stack(1, 2);
            expect(stack.take(3)).to.be.null;
            expect((new Stack()).take(3)).to.be.null;
            
        });
    });

    describe('#isEmpty', () => {
        it('should be empty if it doesn\'t have items in it.', () => {
            const stack = new Stack();
            expect(stack.isEmpty()).to.be.true;
            expect(stack.items).to.be.empty;
        });
    
        it('should not say it\'s empty if it has items in it', () => {
            const stack = new Stack(1);
            expect(stack.isEmpty()).to.be.false;
            expect(stack.items).to.eql([1]);
        });
    });

    describe('#size', () => {
        it('should be able to report its size', () => {
            const stack = new Stack();
    
            expect(stack.size()).to.equal(0);
            stack.push(1);
            expect(stack.size()).to.equal(1);
            stack.push(2,3);
            expect(stack.size()).to.equal(3);
        });
    });

    describe('#peek', () => {
        it('should show what\'s on top of the stack but not actually pop the value', () => {
            const stack = new Stack(1, 2, 3, 4);
            expect(stack.peek()).to.equal(4);
        });

        it('should not pop or otherwise decrease the size of the stack', () => {
            const stack = new Stack(1, 2, 3, 4);
            expect(stack.size()).to.equal(4);
            stack.peek();
            expect(stack.size()).to.equal(4);
        });

        it('should return null if stack is empty', () => {
            const stack = new Stack();
            expect(stack.peek()).to.be.null;
        });
    });

    describe('#peekMany', () => {
        it('should return n items from the top of the stack as a list', () => {
            const stack = new Stack(1, 2, 3, 4);
            expect(stack.peekMany(2)).to.eql([3, 4]);
        });

        it('should not reduce the size of the stack', () => {
            const stack = new Stack(1, 2, 3, 4),
                peeked = stack.peekMany(2);

            expect(stack.items).to.eql([1, 2, 3, 4]);
            expect(peeked).to.eql([3, 4]);
        });

        it('should return null when trying to peek more than the stack has in it', () => {
            const stack = new Stack(1, 2);
            expect(stack.peekMany(3)).to.be.null;
            expect((new Stack()).peekMany(3)).to.be.null;
            
        });
    });

    describe('#peekAll', () => {
        it('should return all the items from the stack as a list', () => {
            const stack = new Stack(1, 2, 3, 4),
                peeked = stack.peekAll();

            expect(stack.items).to.eql([1, 2, 3, 4]);
            expect(peeked).to.eql([1, 2, 3, 4]);
        });

        it('should return no items from the stack if there are no items', () => {
            const stack = new Stack(),
                peeked = stack.peekAll()

            expect(stack.items).to.be.empty;
            expect(peeked).to.be.empty;
        });
    });

    describe('#clone', () => {
        it('should return a new stack with a shallow copy of the stack\'s items', () => {
            const stack = new Stack(1, 2, 3, 4),
                clonedStack = stack.clone();
            expect(stack).to.not.equal(clonedStack);
            expect(stack.items).to.eql(clonedStack.items);
        });
    });
});