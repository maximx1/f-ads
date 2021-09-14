import { Color, Engine } from 'excalibur';
import ResourceManager from './core/gameutils/ResourceManager';
import FreecellGame from './freecell/FreecellGame';

const engine = new Engine({
    backgroundColor: Color.Black
});

//TODO: These should be loaded in a scene but, for the moment we only have freecell.
const resourceManager = new ResourceManager();
resourceManager.setUpResources();

engine.start(resourceManager.loader).then(() => {
    const freecellGame = new FreecellGame(engine, resourceManager.resources);

    freecellGame.startNewGame();
});
