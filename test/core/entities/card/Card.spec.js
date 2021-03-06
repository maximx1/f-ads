import { expect } from 'chai';
import { Card, CardSuit } from '../../../../app/core/entities/card/Card.js';

describe('Card', () => {
    describe('#isValidCard', () => {
        it('should return true if number and suits are both within range', () => {
            expect((new Card(1, CardSuit.SPADES).isValidCard())).to.be.true;
            expect((new Card(2, CardSuit.HEARTS).isValidCard())).to.be.true;
            expect((new Card(7, CardSuit.CLUBS).isValidCard())).to.be.true;
            expect((new Card(13, CardSuit.DIAMONDS).isValidCard())).to.be.true;
        });

        it('should return false if the card number falls out of range', () => {
            expect((new Card(0, CardSuit.SPADES).isValidCard())).to.be.false;
            expect((new Card(14, CardSuit.SPADES).isValidCard())).to.be.false;
            expect((new Card(1000, CardSuit.SPADES).isValidCard())).to.be.false;
        });

        it('should return false if the card suit falls out of range', () => {
            expect((new Card(1, 0).isValidCard())).to.be.false;
            expect((new Card(1, 5).isValidCard())).to.be.false;
            expect((new Card(1, 1000).isValidCard())).to.be.false;
        });

        it('should return false if either the card number or suit is NaN', () => {
            expect((new Card(undefined, 1).isValidCard())).to.be.false;
            expect((new Card('abc123', 1).isValidCard())).to.be.false;
            expect((new Card(1, null).isValidCard())).to.be.false;
            expect((new Card(1, 'abc123').isValidCard())).to.be.false;
        });
    });
});