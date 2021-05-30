import { Actor, Color } from 'excalibur';

export default class EmptyCardSlotActor extends Actor {
    constructor(entity) {
        super();

        this.entity = entity;
        entity.emptyCardSlotActor = this;

        this.initializeVisualConfig();
    }

    initializeVisualConfig() {
        this.color = Color.Vermillion
    }
}