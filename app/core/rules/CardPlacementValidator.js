export default class CardPlacementValidator {
    cardsAreAdjacentNumber(card1, card2, allowWrap) {
        if(card1 == null || card2 == null || !card1.isValidCard() || !card2.isValidCard()) return false;

        if(allowWrap == null) allowWrap = false;

        const delta = Math.abs(card1.number - card2.number);

        if(delta === 1 || (allowWrap && delta === 12)) {
            return true;
        }
        
        return false;
    }

    cardsAreAdjacentBasedOnSuit(card1, card2, alternatingSuit) {
        if(card1 == null || card2 == null || !card1.isValidCard() || !card2.isValidCard()) return false;

        // Reds are even, Blacks are odd: [1,2,3,4]
        // After subtracting, If alternating true would be odd, if not alternating true would be even.
        return Math.abs(card1.suit - card2.suit) % 2 !== (alternatingSuit ? 0 : 1);
    }

    isCardPlaceableOnOtherCard(cardToPlace, cardToPlaceUpon, isDescending, alternatingSuit) {
        if(isDescending == null) isDescending = true;
        if(alternatingSuit == null) alternatingSuit = true;

        var cardDiff = cardToPlaceUpon.number - cardToPlace.number;

        if((isDescending && cardDiff > 0) || (!isDescending && cardDiff < 0)) {
            return this.cardsAreAdjacentNumber(cardToPlace, cardToPlaceUpon) &&
                    this.cardsAreAdjacentBasedOnSuit(cardToPlace, cardToPlaceUpon, alternatingSuit);
        }

        return false;
    }
}