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
        return this.verticalPadding + this.cardHeight + this.gutterSpace;
    }

    updateActors(field) {
        this.updateTableauActors(field);
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