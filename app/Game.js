import { CollisionType, Color, Engine } from 'excalibur';
import CardActor from './core/actors/CardActor'
import FreecellGame from './freecell/FreecellGame';

const engine = new Engine({
    backgroundColor: Color.Black
});

engine.start();
//paddle.body.collider.type = CollisionType.Fixed

const freecellGame = new FreecellGame(engine);

freecellGame.startNewGame();

// engine.input.pointers.primary.on('move', function (evt) {
//     paddle.pos.x = evt.target.lastWorldPos.x
// });


