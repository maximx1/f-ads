export default class FreecellChoreographer {
    constructor(engine) {
        this.engine = engine;
    }

    scalePlayArea() {
        this.defineScaledParameters();
        this.definePlayArea();
        this.defineCardDimensions();
        this.defineHorizontalPadding();
        this.verticalPadding = 20;
    }

    defineScaledParameters() {
        this.scaledParameters = {
            maxPlayAreaInPixels: 700,
            playAreaWidthInUnits: 49,
            cardWidthInUnits: 5,
            cardHeightInUnits: 6,
            gutterWidthInUnits: 1
        };
    }

    definePlayArea() {
        const sp = this.scaledParameters;
        this.playAreaWidth = Math.min(sp.maxPlayAreaInPixels, this.engine.drawWidth);
        this.gutterSpace = Math.floor(this.playAreaWidth / sp.playAreaWidthInUnits * sp.gutterWidthInUnits);
        
    }

    defineCardDimensions() {
        const sp = this.scaledParameters;
        this.cardWidth = Math.floor(sp.cardWidthInUnits * this.playAreaWidth / sp.playAreaWidthInUnits);
        this.cardHeight = Math.floor(sp.cardHeightInUnits * this.cardWidth / sp.cardWidthInUnits);
    }

    defineHorizontalPadding() {
        this.horizontalPadding = Math.round(Math.max(0, this.engine.drawWidth - this.playAreaWidth) / 2);
    }

    getColumnXLocation(columnIndex) {
        return this.horizontalPadding + this.gutterSpace + ((this.gutterSpace + this.cardWidth) * columnIndex);
    }

    getColumnYLocation() {
        return this.getTopRowYLocation() + this.cardHeight + this.gutterSpace;
    }

    getTopRowYLocation() {
        return this.verticalPadding;
    }

    updateActors(field) {
        this.updateTableauActors(field);
        this.updateFreecellActors(field);
        this.updateFoundationActors(field);
    }

    updateFoundationActors(field) {
        const topRowYLocation = this.getTopRowYLocation();

        field.foundations.forEach((foundation, index) => {
            const foundationXPosition = this.getColumnXLocation(index + 4);
            foundation.getActiveActors().forEach(actor => {
                {
                    actor.pos.x = foundationXPosition;
                    actor.pos.y = topRowYLocation;;
                    actor.width = this.cardWidth;
                    actor.height = this.cardHeight;
                }
            }, this);
        }, this);
    }

    updateFreecellActors(field) {
        const topRowYLocation = this.getTopRowYLocation();

        field.freeCells.forEach((freeCell, index) => {
            const freecellXPosition = this.getColumnXLocation(index);
            freeCell.getActiveActors().forEach(actor => {
                {
                    actor.pos.x = freecellXPosition;
                    actor.pos.y = topRowYLocation;;
                    actor.width = this.cardWidth;
                    actor.height = this.cardHeight;
                }
            }, this);
        }, this);
    }

    updateTableauActors(field) {
        const columnYPosition = this.getColumnYLocation();
        field.tableau.forEach((column, columnIndex) => {
            const columnXPosition = this.getColumnXLocation(columnIndex);
            column.peekAll().forEach((card, cardIndex) => {
                card.actors.forEach(actor => {
                    actor.pos.x = columnXPosition;
                    actor.pos.y = columnYPosition + this.cardHeight / 2 * cardIndex;
                    actor.width = this.cardWidth;
                    actor.height = this.cardHeight;
                }, this);
            }, this);
        }, this);
    }
}