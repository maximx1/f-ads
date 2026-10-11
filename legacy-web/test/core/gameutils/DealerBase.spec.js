import { expect } from 'chai';
import { Card, CardSuit } from '../../../app/core/entities/card/Card';
import DealerBase from '../../../app/core/gameutils/DealerBase.js';

describe('Dealer', () => {
    it('should be initialized with a brand new kissing king deck', () => {
        const dealer = new DealerBase(),
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
        const dealer = new DealerBase(),
            originalDeck = dealer.deck.slice();

        dealer.shuffleCards();
        expect(dealer.deck).to.not.eql(originalDeck);
        expect(dealer.deck).to.have.lengthOf(52);
        expect(dealer.deck).to.have.members(originalDeck);
    });

    describe('should be able to get cards from each suit in their kissing king order', () => {
        const dealer = new DealerBase(),
            expectedSpadeAndDiamondNumberOrder = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13],
            expectedClubsAndHeartsNumberOrder = [13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1];

        it('spades:', () => {
            const cards = dealer.getCardsFromSuit(CardSuit.SPADES);

            expect(cards.filter(card => card.suit != CardSuit.SPADES)).to.be.an('array').that.is.empty;
            expect(cards.map(card => card.number)).to.eql(expectedSpadeAndDiamondNumberOrder);
        });

        it('diamonds:', () => {
            const cards = dealer.getCardsFromSuit(CardSuit.DIAMONDS);

            expect(cards.filter(card => card.suit != CardSuit.DIAMONDS)).to.be.an('array').that.is.empty;
            expect(cards.map(card => card.number)).to.eql(expectedSpadeAndDiamondNumberOrder);
        });

        it('clubs:', () => {
            const cards = dealer.getCardsFromSuit(CardSuit.CLUBS);

            expect(cards.filter(card => card.suit != CardSuit.CLUBS)).to.be.an('array').that.is.empty;
            expect(cards.map(card => card.number)).to.eql(expectedClubsAndHeartsNumberOrder);
        });

        it('hearts:', () => {
            const cards = dealer.getCardsFromSuit(CardSuit.HEARTS);

            expect(cards.filter(card => card.suit != CardSuit.HEARTS)).to.be.an('array').that.is.empty;
            expect(cards.map(card => card.number)).to.eql(expectedClubsAndHeartsNumberOrder);
        });

        it('should return null should an invalid suit be passed in', () => {
            expect(dealer.getCardsFromSuit(-1)).to.be.null;
            expect(dealer.getCardsFromSuit()).to.be.null;
            expect(dealer.getCardsFromSuit(null)).to.be.null;
        });
    });

});