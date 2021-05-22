export class Card {
    constructor(number, suit) {
        this.number = number;
        this.suit = suit;
    }

    isValidCard() {
        return this.number <= 13 && this.number >=1 && this.suit <= 4 && this.suit >= 1;
    }
}

export class CardSuit {
    static SPADES = 1;
    static HEARTS = 2;
    static CLUBS = 3;
    static DIAMONDS = 4;
}