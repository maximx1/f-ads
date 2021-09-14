import { Loader, Texture } from 'excalibur';
import cardBackTexture from '../assets/cardback.svg';

export default class ResourceManager {
    constructor() {
        this.resources = {};
        this.loader = new Loader();
    }

    setUpResources() {
        this.setUpTexturesForLoad();
    }

    setUpTexturesForLoad() {
        this.resources['cardBack'] = new Texture(cardBackTexture);
        this.loader.addResource(this.resources.cardBack);
    }
}