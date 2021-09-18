import { Loader, Texture } from 'excalibur';
import cardTextures from '../assets/CardTextures';

export default class ResourceManager {
    constructor() {
        this.resources = {};
        this.loader = new Loader();
    }

    setUpResources() {
        this.setUpTexturesForLoad();
    }

    setUpTexturesForLoad() {
        for (let [key, value] of Object.entries(cardTextures)) {
            this.resources[key] = new Texture(value);
            this.loader.addResource(this.resources[key]);
        }
        


        // this.resources['cardBack'] = new Texture(cardBackTexture);
        // this.loader.addResource(this.resources.cardBack);

        // this.resources['aceOfSpades'] = new Texture(aceOfSpades);
        // this.loader.addResource(this.resources.aceOfSpades);
    }
}