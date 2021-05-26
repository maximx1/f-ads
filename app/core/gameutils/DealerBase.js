import { Card, CardSuit } from "../Entities/Card/Card";

export default class DealerBase {
    constructor() {
        this.deck = this.getFreshDeck();
    }

    shuffle(playField) { }

    shuffleCards() {
        let currentIndex = this.deck.length;

        while(0 !== currentIndex) {
            const randomIndex = Math.floor(Math.random() * currentIndex),
                tmp = this.deck[--currentIndex];
            this.deck[currentIndex] = this.deck[randomIndex];
            this.deck[randomIndex] = tmp;
        }
    }

    getFreshDeck() {
        return this.getFreshKissingKingDeck();
    }

    getFreshKissingKingDeck() {
        const cards = [];
        cards.push(...this.getCardsFromSuit(CardSuit.SPADES));
        cards.push(...this.getCardsFromSuit(CardSuit.DIAMONDS));
        cards.push(...this.getCardsFromSuit(CardSuit.CLUBS));
        cards.push(...this.getCardsFromSuit(CardSuit.HEARTS));
        return cards;
    }

    getCardsFromSuit(suit) {
        switch(suit) {
            case CardSuit.SPADES:
            case CardSuit.DIAMONDS:
                return this.getCardsForSuitInOrder(suit, false);
            case CardSuit.CLUBS:
            case CardSuit.HEARTS:
                return this.getCardsForSuitInOrder(suit, true);
            default:
                return null;
        }
    }

    getCardsForSuitInOrder(suit, reverse) {
        const cards = [];
        if(reverse) {
            for(let i = 13; i >= 1; i--) {
                cards.push(new Card(i, suit));
            }
            
        } else {
            for(let i = 1; i <= 13; i++) {
                cards.push(new Card(i, suit));
            }
        }
        return cards;
    }
}