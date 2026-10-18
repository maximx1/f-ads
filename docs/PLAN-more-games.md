# Plan: Solitaire, Spider, a game picker, a tidier menu, and a searchable history

## Context

f-ads is FreeCell only. Every layer assumes it: `GameState` (8 columns, 4 free cells), `Move`, `Deal`, `Rules`, `Game`/`GameRecord`, `BoardLayout`/`Board`/`Hand` in `ui/`, and one save file.

You asked for:
- Klondike Solitaire with a draw 1 or draw 3 option.
- Spider with a 1, 2 or 4 suit option.
- A landing menu on first open to pick the game. After that the game type stays put until you change it in the New game dialog.
- A cleaner pause menu, with the theme picked from a dropdown.
- A searchable history of every game (deal number, time, moves, score), with **Play again** and **Watch** next to each one.

Your answers so far:
- Replay means both Play again and Watch.
- The history covers all three games.
- Klondike gets a draw 1/3 option.
- Each game scores by its classic rule.

Auto-complete for FreeCell is built and passes `check` but is not committed. It still needs to go on the phone, which was unplugged.

## Step 0: Finish auto-complete
- Commit the FreeCell auto-complete (rule, button second from left, clock freezes on press) to `34-timed-play`.
- Cut a new branch for everything below, `35-more-games`, from `34-timed-play`.
- Commit at the end of each step below.

## Step 1: A game-agnostic core
- New `core/.../engine/Rules.kt`: `interface Rules<S, M>`. Each game implements:
  - `deal(number)`, `play(state, move)`, `canMove`, `isWon`
  - `hint(state, before)`, `nextAutoMove(state)` (null when it can't finish itself)
  - `score(game)`, and `moveSerializer`
- New `GameType` (serializable sealed type): `FreeCell`, `Klondike(draw = 1 | 3)`, `Spider(suits = 1 | 2 | 4)`. It has a label for the UI and a `rules` instance.
- `Game` and `GameRecord` become generic over state and move. Undo, the move count, hints, the clock, `inPlay` and `withElapsed` stay as they are.
  - `GameRecord` gains a `type` field that defaults to FreeCell, so today's `game.json` still loads. `ignoreUnknownKeys` is already on.
- FreeCell: `FreeCellRules` wraps the existing functions. These are `Engine.play`, `Rules.canMove`, `Hint.hint`, `AutoComplete.nextAutoMove` and `Scoring`. The existing FreeCell packages stay where they are, so the diff stays small.
- Cards with identity: Spider deals two decks, so a `Card` alone can't key a card on screen.
  - Add `core/model/Piece(id: Int, card: Card, faceUp: Boolean)`.
  - Klondike and Spider states hold pieces. FreeCell keeps `Card` and its board adapter uses the deck index as the id.
- Shuffling: pull the SplitMix64 Fisher–Yates out of `Deal.fromNumber` into a shared `shuffled(deck, number)`. FreeCell's layouts must not change; the existing fixture tests lock them.
- `Records` becomes one set per game type.
  - FreeCell keeps `best-times.json`.
  - The others get files such as `records-klondike-3.json` and `records-spider-2.json`.

## Step 2: A board any game can use
- `ui/Board.kt` now drives FreeCell's `BoardLayout`, `Spot`, `DropTarget` and `Hand` directly. Split it in two:
  - A generic board that draws pieces at positions (face up or down), slot outlines and hint marks. It keeps the existing glide, drag, lift and dim behaviour.
  - A per-game `TableLayout<S, M>` that answers where each piece sits, where the slots are, what a tap on a piece or slot plays, what a drag picks up, and what a drop at a point plays.
- FreeCell's `TableLayout` is today's `BoardLayout`, `tapMoveFromColumn`, `tapMoveFromFreeCell`, `moveFor` and `targetAt`, moved behind the interface with no change in behaviour.
- Card backs: `CardStyle` gains `drawBack()` with a plain default.
  - Each `Look` gets a back to suit its theme: Aqua pinstripe for brushed metal, glass for Vista, neon grid for cyberpunk, worn red for the back room, plain for flat.
- Slots can carry a mark: a suit for a foundation, a recycle arrow for an empty stock.

## Step 3: Game picker, New game options, menu cleanup
- `Settings` gains `gameType`, which has no default. The view model holds the current type and keeps one save per type: `game.json` for FreeCell, `klondike.json`, `spider.json`.
- **Landing menu.** It shows only while no game type has been saved, which means on first open.
  - Three large choices: FreeCell, Solitaire, Spider.
  - Under Solitaire, a Draw 1 / Draw 3 choice. Under Spider, 1 / 2 / 4 suits: 1 suit is all spades, 2 suits are spades and hearts, 4 suits is the full deck.
  - Picking one deals a random game and saves the choice.
- **New game dialog.** Same game and option pickers at the top, preselected to the current type, then the deal number field, Random deal and Cancel. Dealing a different type switches modes and saves it. Nothing else ever switches it.
- **Pause menu cleanup** (`ui/Dialogs.kt`), in this order:
  1. *This game*: game and option, deal number, time, moves, score.
  2. *Records* for this game type and this deal.
  3. A **History** button.
  4. *Settings*: the theme as an `ExposedDropdownMenuBox`, and the dim switch.
- Bottom bar for all games: Undo, Auto-complete, Restart, New, Hint, Pause. Auto-complete stays disabled when a game can't finish itself.

## Step 4: Klondike
- `core/klondike/`:
  - **State**: 7 tableau piles of pieces, stock, waste, foundations, and a recycle count.
  - **Moves**: draw, recycle the waste, waste or tableau to foundation, waste to tableau, tableau run to tableau, foundation back to tableau. Turning a card over happens by itself when it is uncovered.
  - **Deal**: 1–7 cards per pile, last card face up, and 24 left in the stock.
  - **Hint**: a simple priority, like FreeCell's.
  - **Auto-complete**: available once the stock and waste are empty and every tableau card is face up.
- **Scoring**: Windows standard, worked out by replaying the record, so an undo takes its points back.
  - Waste to tableau +5. Waste or tableau to foundation +10. Turning a card over +5. Foundation back to tableau −15.
  - Recycling the waste costs −100 with draw 1 and −20 with draw 3.
  - −2 for every 10 seconds, and on a win the 700,000 ÷ seconds bonus. The score never goes below 0.
- **Layout**: stock, waste (draw 3 fans its top three) and 4 foundations in the top row. Tableau below, with face-down cards fanned tighter than face-up ones.
  - Tapping the stock draws, or recycles when the stock is empty. Tapping a card plays its best move.

## Step 5: Spider
- `core/spider/`:
  - **State**: 10 piles of pieces, the stock (5 deals of 10), and the count of finished suits.
  - **Moves**: move a same-suit descending run onto any card one rank higher, or onto an empty pile. Deal a row of 10, which is only allowed when no pile is empty. A finished K→A run of one suit leaves the table by itself.
  - **Deal**: 104 cards, 6 to each of the first 4 piles and 5 to the other 6, top card face up.
    - 1 suit means 8 decks' worth of spades. 2 suits means spades and hearts, 4 decks' worth of each. 4 suits is two full decks.
  - **Auto-complete**: available once every card is face up, the stock is empty, and each pile is one same-suit descending run.
- **Scoring**: start at 500, −1 for every move made (undos count as moves, as in Windows), +100 for every finished suit.
- **Layout**: 10 columns, with the stock bottom-right showing how many deals are left and the finished suits stacked bottom-left. Cards are narrower than FreeCell's to fit 10 across an upright phone.

## Step 6: History, Play again and Watch
- `core/engine/History.kt`: one entry per game that ends with at least one move made. A game ends when it is won, or when it is abandoned by New game or Restart. Each entry holds:
  - game type, deal number, when it was played
  - time, moves made, score, won or not
  - the recorded moves, so Watch can play them back
- Saved as `history.json`, newest first. That is about 4 KB per FreeCell game. Splitting it into several files can wait until it grows.
- **History screen**, full screen, opened from the pause menu:
  - A search field that matches deal numbers.
  - Filter chips for each game, and a won-only toggle.
  - A list showing game and option, deal number, date, time, moves, score and won mark.
  - On each row: **Play again** (same type and deal, fresh start) and **Watch**.
- **Watch screen**: the board with touch input switched off. Controls to go to the start, step back, play or pause, and step forward, plus a speed toggle and "move n of N".
  - It reuses `GameRecord.replay()`, stepping through the moves of the generic `Game`.
- Navigation is a small screen enum in `GameScreen`: landing, table, history, watch. No navigation library.

## Critical files
- **Core**: `core/.../engine/{Game,GameRecord,Records,Scoring}.kt`, `deal/Deal.kt`, plus new `engine/{Rules,GameType,History}.kt`, `model/Piece.kt`, `klondike/*`, `spider/*`.
- **UI**: `ui/.../ui/{Board,BoardLayout,GameScreen,Dialogs,Strings,Settings,CardStyle,Glyph}.kt`, `ui/.../game/{GameViewModel,GameStore}.kt`, `ui/.../ui/theme/*Look.kt` (card backs), plus new `Landing.kt`, `History.kt`, `Watch.kt`, `TableLayout.kt`.
- **Shells** stay thin: `androidApp/.../MainActivity.kt` and `desktopApp/.../Main.kt` only pass the same `Settings`.

## Verification
- Run `./gradlew check` after every step. It covers detekt, spotless, core JVM and Android host tests, Robolectric UI tests and the desktop tests.
- **Core tests for each game**:
  - Fixture tests that lock deals 1, 2 and a large number for each variant.
  - Move legality.
  - A scripted or random-play win: random play must never reach an illegal state.
  - Scoring cases, auto-complete, and save round-trips.
  - FreeCell's existing tests must pass unchanged, which proves the refactor kept its behaviour.
- **UI tests** in `GameScreenTest`:
  - The landing shows only on first open.
  - The game type survives a restart of the app and changes only from New game.
  - A tap and a drag work in each game.
  - The history search filters, Play again deals the same number, and Watch steps through the moves.
  - `ThemeTest` runs for each theme and each game, so the screenshots in `androidApp/build/reports/themes` can be checked by eye.
- **On the phone**: install with `./gradlew :androidApp:installDebug`. Play a game of each type, check auto-complete, finish a game and find it in the history, Watch it, then Play it again.
