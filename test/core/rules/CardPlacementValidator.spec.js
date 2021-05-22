import { expect } from 'chai';
import CardPlacementValidator from '../../../app/core/rules/CardPlacementValidator.js';
import { Card, CardSuit } from '../../../app/core/entities/card/Card.js';

var validator = new CardPlacementValidator();

describe('Adjacent cards determinator', () => {
    describe('Adjacent by number', () => {
        it('should return true if 2 numbers are next to each other', () => {
            const card1 = new Card(1, CardSuit.SPADES),
                card2 = new Card(2, CardSuit.HEARTS);
            expect(validator.cardsAreAdjacentNumber(card1, card2)).to.equal(true);
        });

        it('should return false if 2 numbers are not next to each other', () => {
            const card1 = new Card(1, CardSuit.SPADES),
                card2 = new Card(3, CardSuit.HEARTS);
            expect(validator.cardsAreAdjacentNumber(card1, card2)).to.equal(false);
        });

        it('should return true if 1 card is A and the other is K and wrap is enabled', () => {
            const card1 = new Card(1, CardSuit.SPADES),
                card2 = new Card(13, CardSuit.HEARTS);
            expect(validator.cardsAreAdjacentNumber(card1, card2, true)).to.equal(true);
        });

        it('should return false if 1 card is A and the other is K but wrap is not enabled', () => {
            const card1 = new Card(1, CardSuit.SPADES),
                card2 = new Card(13, CardSuit.HEARTS);
            expect(validator.cardsAreAdjacentNumber(card1, card2, false)).to.equal(false);
        });
    });

    describe('Adjacent by suit', () => {
        // Black
        const spadesCard = new Card(1, CardSuit.SPADES),
            clubsCard = new Card(2, CardSuit.CLUBS),

        // Red
            heartsCard = new Card(3, CardSuit.HEARTS),
            diamondsCard = new Card(4, CardSuit.DIAMONDS);

        describe('Alternating suits', () => {
            

            it('should return true for red on black', () => {
                expect(validator.cardsAreAdjacentBasedOnSuit(heartsCard, spadesCard, true)).to.equal(true);
                expect(validator.cardsAreAdjacentBasedOnSuit(heartsCard, clubsCard, true)).to.equal(true);
                expect(validator.cardsAreAdjacentBasedOnSuit(diamondsCard, spadesCard, true)).to.equal(true);
                expect(validator.cardsAreAdjacentBasedOnSuit(diamondsCard, clubsCard, true)).to.equal(true);
            });

            it('should return true for black on red', () => {
                expect(validator.cardsAreAdjacentBasedOnSuit(spadesCard, heartsCard, true)).to.equal(true);
                expect(validator.cardsAreAdjacentBasedOnSuit(spadesCard, diamondsCard, true)).to.equal(true);
                expect(validator.cardsAreAdjacentBasedOnSuit(clubsCard, heartsCard, true)).to.equal(true);
                expect(validator.cardsAreAdjacentBasedOnSuit(clubsCard, diamondsCard, true)).to.equal(true);
            });

            it('should return false for black on black', () => {
                expect(validator.cardsAreAdjacentBasedOnSuit(clubsCard, spadesCard, true)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(spadesCard, clubsCard, true)).to.equal(false);
            });

            it('should return false for red on red', () => {
                expect(validator.cardsAreAdjacentBasedOnSuit(heartsCard, diamondsCard, true)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(diamondsCard, heartsCard, true)).to.equal(false);
            });

            it('should return false if either card is invalid', () => {
                expect(validator.cardsAreAdjacentBasedOnSuit()).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(spadesCard)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(null, spadesCard)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(spadesCard, new Card(-1, -1))).to.equal(false);
            });
        });

        describe('Non-alternating suits', () => {
            it('should return false for red on black', () => {
                expect(validator.cardsAreAdjacentBasedOnSuit(heartsCard, spadesCard, false)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(heartsCard, clubsCard, false)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(diamondsCard, spadesCard, false)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(diamondsCard, clubsCard, false)).to.equal(false);
            });

            it('should return false for black on red', () => {
                expect(validator.cardsAreAdjacentBasedOnSuit(spadesCard, heartsCard, false)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(spadesCard, diamondsCard, false)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(clubsCard, heartsCard, false)).to.equal(false);
                expect(validator.cardsAreAdjacentBasedOnSuit(clubsCard, diamondsCard, false)).to.equal(false);
            });

            it('should return true for black on black', () => {
                expect(validator.cardsAreAdjacentBasedOnSuit(clubsCard, spadesCard, false)).to.equal(true);
                expect(validator.cardsAreAdjacentBasedOnSuit(spadesCard, clubsCard, false)).to.equal(true);
            });

            it('should return true for red on red', () => {
                expect(validator.cardsAreAdjacentBasedOnSuit(heartsCard, diamondsCard, false)).to.equal(true);
                expect(validator.cardsAreAdjacentBasedOnSuit(diamondsCard, heartsCard, false)).to.equal(true);
            });
        });
    });

    describe('#isCardPlaceableOnOtherCard', () => {
        describe('Descending', () => {
            describe('Alternating suits', () => {
                const runTestCase = (c1Number, c1Suit, c2Number, c2Suit, result) => {
                    const cardToPlace = new Card(c1Number, c1Suit),
                        cardToPlaceUpon = new Card(c2Number, c2Suit);

                    expect(validator.isCardPlaceableOnOtherCard(cardToPlace, cardToPlaceUpon, true, true)).to.equal(result);
                };

                it('should return true if card can be placed upon', () => {
                    runTestCase(12, CardSuit.SPADES, 13, CardSuit.DIAMONDS, true);
                });

                it('should return false if card to be placed upon is the same number as the one to place', () => {
                    runTestCase(13, CardSuit.SPADES, 13, CardSuit.DIAMONDS, false);
                });

                it('should return false if card to be placed upon is smaller than card to place', () => {
                    runTestCase(13, CardSuit.SPADES, 12, CardSuit.DIAMONDS, false);
                });

                it('should return false if card to be placed upon is the same color suit', () => {
                    runTestCase(12, CardSuit.SPADES, 13, CardSuit.CLUBS, false);
                    runTestCase(12, CardSuit.SPADES, 13, CardSuit.SPADES, false);
                    runTestCase(12, CardSuit.HEARTS, 13, CardSuit.DIAMONDS, false);
                });
            });
    
            describe('Non-alternating suits', () => {
                const runTestCase = (c1Number, c1Suit, c2Number, c2Suit, result) => {
                    const cardToPlace = new Card(c1Number, c1Suit),
                        cardToPlaceUpon = new Card(c2Number, c2Suit);

                    expect(validator.isCardPlaceableOnOtherCard(cardToPlace, cardToPlaceUpon, true, false)).to.equal(result);
                };

                it('should return true if card can be placed upon', () => {
                    runTestCase(12, CardSuit.SPADES, 13, CardSuit.SPADES, true);
                    runTestCase(12, CardSuit.SPADES, 13, CardSuit.CLUBS, true);
                });

                it('should return false if card to be placed upon is the same number as the one to place', () => {
                    runTestCase(13, CardSuit.DIAMONDS, 13, CardSuit.DIAMONDS, false);
                });

                it('should return false if card to be placed upon is smaller than card to place', () => {
                    runTestCase(13, CardSuit.DIAMONDS, 12, CardSuit.DIAMONDS, false);
                });

                it('should return false if card to be placed upon is a different color suit', () => {
                    runTestCase(12, CardSuit.SPADES, 13, CardSuit.HEARTS, false);
                });
            });
        });

        describe('Ascending', () => {
            describe('Alternating suits', () => {
                const runTestCase = (c1Number, c1Suit, c2Number, c2Suit, result) => {
                    const cardToPlace = new Card(c1Number, c1Suit),
                        cardToPlaceUpon = new Card(c2Number, c2Suit);

                    expect(validator.isCardPlaceableOnOtherCard(cardToPlace, cardToPlaceUpon, false, true)).to.equal(result);
                };

                it('should return true if card can be placed upon', () => {
                    runTestCase(2, CardSuit.SPADES, 1, CardSuit.DIAMONDS, true);
                });

                it('should return false if card to be placed upon is the same number as the one to place', () => {
                    runTestCase(1, CardSuit.SPADES, 1, CardSuit.DIAMONDS, false);
                });

                it('should return false if card to be placed upon is larger than card to place', () => {
                    runTestCase(3, CardSuit.SPADES, 4, CardSuit.DIAMONDS, false);
                });

                it('should return false if card to be placed upon is the same color suit', () => {
                    runTestCase(2, CardSuit.SPADES, 1, CardSuit.CLUBS, false);
                    runTestCase(2, CardSuit.SPADES, 1, CardSuit.SPADES, false);
                    runTestCase(2, CardSuit.HEARTS, 1, CardSuit.DIAMONDS, false);
                });
            });
    
            describe('Non-alternating suits', () => {
                const runTestCase = (c1Number, c1Suit, c2Number, c2Suit, result) => {
                    const cardToPlace = new Card(c1Number, c1Suit),
                        cardToPlaceUpon = new Card(c2Number, c2Suit);

                    expect(validator.isCardPlaceableOnOtherCard(cardToPlace, cardToPlaceUpon, false, false)).to.equal(result);
                };

                it('should return true if card can be placed upon', () => {
                    runTestCase(2, CardSuit.SPADES, 1, CardSuit.SPADES, true);
                    runTestCase(2, CardSuit.SPADES, 1, CardSuit.CLUBS, true);
                });

                it('should return false if card to be placed upon is the same number as the one to place', () => {
                    runTestCase(13, CardSuit.DIAMONDS, 13, CardSuit.DIAMONDS, false);
                });

                it('should return false if card to be placed upon is larfer than card to place', () => {
                    runTestCase(3, CardSuit.DIAMONDS, 4, CardSuit.DIAMONDS, false);
                });

                it('should return false if card to be placed upon is a different color suit', () => {
                    runTestCase(12, CardSuit.SPADES, 13, CardSuit.HEARTS, false);
                });
            });
        });
    });
});