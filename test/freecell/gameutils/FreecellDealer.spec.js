import { expect } from 'chai';
import { CardSuit } from '../../../app/core/Entities/Card/Card.js';
import FreecellDealer from '../../../app/freecell/gameutils/FreecellDealer.js';
import { Field } from '../../../app/freecell/gameutils/Field.js';


describe('Freecell Dealer', () => {
    it('should be able to deal the deck to a freecell field', () => {
        const dealer = new FreecellDealer(),
            field = new Field(),
            assertFieldColumn = (column, expectedCardNumbers, expectedSuits) => {
                expect(column.items.map(card => card.number)).to.eql(expectedCardNumbers);
                expect(column.items.map(card => card.suit)).to.eql(expectedSuits);
            },
            spades = CardSuit.SPADES,
            diamonds = CardSuit.DIAMONDS,
            clubs = CardSuit.CLUBS,
            hearts = CardSuit.HEARTS;

        dealer.deal(field);

        assertFieldColumn(field.columns[0],
            [1, 9, 4, 12, 7, 12, 4],
            [spades, spades, diamonds, diamonds, clubs, hearts, hearts]
        );
        assertFieldColumn(field.columns[1],
            [2, 10, 5, 13, 6, 11, 3],
            [spades, spades, diamonds, diamonds, clubs, hearts, hearts]
        );
        assertFieldColumn(field.columns[2],
            [3, 11, 6, 13, 5, 10, 2],
            [spades, spades, diamonds, clubs, clubs, hearts, hearts]
        );
        assertFieldColumn(field.columns[3],
            [4, 12, 7, 12, 4, 9, 1],
            [spades, spades, diamonds, clubs, clubs, hearts, hearts]
        );
        assertFieldColumn(field.columns[4],
            [5, 13, 8, 11, 3, 8],
            [spades, spades, diamonds, clubs, clubs, hearts]
        );
        assertFieldColumn(field.columns[5],
            [6, 1, 9, 10, 2, 7],
            [spades, diamonds, diamonds, clubs, clubs, hearts]
        );
        assertFieldColumn(field.columns[6],
            [7, 2, 10, 9, 1, 6],
            [spades, diamonds, diamonds, clubs, clubs, hearts]
        );
        assertFieldColumn(field.columns[7],
            [8, 3, 11, 8, 13, 5],
            [spades, diamonds, diamonds, clubs, hearts, hearts]
        );
    });

    it('should not populate any of the freecells when dealing', () => {
        const dealer = new FreecellDealer(),
            field = new Field();

        dealer.shuffleCards();
        dealer.deal(field);

        expect(field.freeCells).to.eql([null, null, null, null]);
    });
});