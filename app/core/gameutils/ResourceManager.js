import { Loader, Texture } from 'excalibur';
import cardBackTexture from '../assets/cardback.svg';
import aceOfSpades from '../assets/ace_of_spades.svg';

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

        this.resources['aceOfSpades'] = new Texture(aceOfSpades);
        this.loader.addResource(this.resources.aceOfSpades);
    }
}