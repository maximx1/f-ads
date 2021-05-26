import { expect } from 'chai';
import { Card, CardSuit } from '../../../app/core/Entities/Card/Card';
import Dealer from '../../../app/core/gameutils/Dealer';

describe('Dealer', () => {
    it('should be initialized with a brand new kissing king deck', () => {
        const dealer = new Dealer(),
            cardNumbers = dealer.deck.map(card => card.number),
            cardSuits = dealer.deck.map(card => card.suit);

        expect(dealer.deck).to.have.lengthOf(52);
        expect(cardNumbers).to.eql([
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
            13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1,
            13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1
        ]);

        const spades = CardSuit.SPADES,
            diamonds = CardSuit.DIAMONDS,
            clubs = CardSuit.CLUBS,
            hearts = CardSuit.HEARTS;

        expect(cardSuits).to.eql([
            spades, spades, spades, spades, spades, spades, spades, spades, spades, spades, spades, spades, spades,
            diamonds, diamonds, diamonds, diamonds, diamonds, diamonds, diamonds, diamonds, diamonds, diamonds, diamonds, diamonds, diamonds,
            clubs, clubs, clubs, clubs, clubs, clubs, clubs, clubs, clubs, clubs, clubs, clubs, clubs,
            hearts, hearts, hearts, hearts, hearts, hearts, hearts, hearts, hearts, hearts, hearts, hearts, hearts
        ]);
    });

    it('should be able to shuffle its deck', () => {
        const dealer = new Dealer(),
            originalDeck = dealer.deck.slice();

        dealer.shuffleCards();
        expect(dealer.deck).to.not.eql(originalDeck);
        expect(dealer.deck).to.have.lengthOf(52);
        expect(dealer.deck).to.have.members(originalDeck);
    });
});