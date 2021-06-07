import { expect } from 'chai';
import Foundation from '../../../app/core/entities/Foundation';

describe('foundation', () => {
    it('should be initialized with an empty stack', () => {
        const foundation = new Foundation();
        expect(foundation.pile.items).to.be.an('array').that.is.empty;
    });

    it('should return the empty slot actor if the foundation pile is empty', () => {
        const foundation = new Foundation(),
            mockedEmptyCardSlotActor = new class {
                constructor(entity) {
                    this.entity = entity;
                    entity.emptyCardSlotActor = this;
                }
            } (foundation);

        expect(foundation.getActiveActors()).to.eql([mockedEmptyCardSlotActor]);
    });

    // TODO: test retrieving nested actors
});