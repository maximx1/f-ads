import { expect } from 'chai';
import FreeCell from '../../../app/freecell/entities/FreeCell'
import { Card, CardSuit } from '../../../app/core/entities/card/Card'

describe('Free Cell', () => {
    it('should be free if there is no card in it', () => {
        const freeCell = new FreeCell();
        expect(freeCell.isFree()).to.be.true;
    });

    it('should not be free if there is a card in it', () => {
        const freeCell = new FreeCell(),
            card = new Card(1, CardSuit.SPADES);
        freeCell.placeCard(card);
        expect(freeCell.isFree()).to.be.false;
    });

    it('should be initialized without any card', () => {
        const freeCell = new FreeCell();
        expect(freeCell.isFree()).to.be.true;
    });

    describe('retrievingActors', () => {
        it('should return the empty slot actor if the free cell has no card in it', () => {
            const freeCell = new FreeCell(),
                mockedEmptyCardSlotActor = new class {
                    constructor(entity) {
                        this.entity = entity;
                        entity.emptyCardSlotActor = this;
                    }
                } (freeCell);

            expect(freeCell.getActiveActors()).to.eql([mockedEmptyCardSlotActor]);
        });
    });

    describe('placing cards on the free cell', () => {
        it('should return true and set the card to free cell if free cell is empty', () => {
            const freeCell = new FreeCell(),
                card = new Card(1, CardSuit.SPADES);
            expect(freeCell.placeCard(card)).to.be.true;
            expect(freeCell.isFree()).to.be.false;
            expect(freeCell.card).to.eql(card);
        });

        it('should return false and not change the card if a free cell is not empty', () => {
            const freeCell = new FreeCell(),
                card1 = new Card(1, CardSuit.SPADES),
                card2 = new Card(2, CardSuit.HEARTS);
            expect(freeCell.placeCard(card1)).to.be.true;
            expect(freeCell.placeCard(card2)).to.be.false;
            expect(freeCell.card).to.eql(card1);
        });
    });

    describe('picking up a card from the free cell', () => {
        it('should return a card held in the free cell', () => {
            const freeCell = new FreeCell(),
                card = new Card(1, CardSuit.SPADES);
            freeCell.placeCard(card);
            expect(freeCell.pickUpCard()).to.eql(card);
        });

        it('should return null if there is no card on the free cell', () => {
            const freeCell = new FreeCell();
            expect(freeCell.pickUpCard()).to.be.null;
        });

        it('should result in the freecell having no card after having picked up a card', () => {
            const freeCell = new FreeCell(),
                card = new Card(1, CardSuit.SPADES);
            freeCell.placeCard(card);
            expect(freeCell.card).to.eql(card);
            expect(freeCell.pickUpCard()).to.eql(card);
            expect(freeCell.card).to.be.null;
        });
    });
    // TODO: test retrieving nested actors
});