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

    // TODO: test retrieving nested actors
});