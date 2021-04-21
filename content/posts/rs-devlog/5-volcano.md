---
title: "Make-it...Tuesday? (Rogueslide Devlog #5)"
date: 2021-04-20
draft: false
summary: "This week was big: The volcano, status effects, audio, and a PC/Mac/Linux build pipeline.  I'll try to cover it all!"
---

I'm a day late to this week's update, but that's not because I accomplished less.  In fact, the opposite is true! I got so wrapped up in working on the game that I didn't want to stop that momentum to write about it.

This week we hit a large milestone in Rogueslide development -- we finished implementing all the enemies from our game design doc.  This means you can play the game from start to finish, and most of our "content" middle section of development is done.

There's still much to be done, but this milestone gave me a huge amount of momentum.  I used that momentum to fill out the game with all the audio assets we have on hand, and to create an automated build pipeline for the desktop build of our game.  The game has come very far in one week!

## New Area: The Volcano

[Last week, I wrote about the dungeon.](/posts/rs-devlog/4-dungeon)  If the theme of that level is "reanimation", then the theme of this volcano level is (predictably) "fire".

![Animated GIF of volcano level in rogueslide](/rs-devlog/5-volcano/volcano.gif "In the volcano, almost everything is flame-resistant.  And you are not.")

The volcano area is the final area of the game, so enemies are tougher here than anywhere else.  Enemies spawn with more HP than in the other two areas, and almost every enemy does more damage.  We want the player to feel a big sense of accomplishment when they reach new areas, so we try to combine increasingly higher tile stats with increasingly difficult enemy abilities to scale the difficulty for each area.  

The hope is that the player will get just a little bit further with each run, and that they will be able to see their own progression over time as they keep bouncing off the game.  This is definitely a difficult line to walk.  Too easy, and the player will lose interest.  Too hard, and the player will bounce off of the game, and not come back.  But my hope is that by rewarding the player with stat boosts, items, and power-ups, we can help them scale together with the difficulty in meaningful and interesting ways.  (Items and experience/leveling-up are in development -- they're next on my list!)

## Volcano Enemies

Here are the enemies in volcano area:

* *Lava Stones*: These are just like every other stone in the game, but when they break, they leave behind a "lava" trap tile.  When an enemy slides over the trap, they take fire damage, and if they are flammable, they stay on fire for the following 3 turns, taking damage each turn.
* *Lava Slimes*: A higher-stat version of the slime with red volcano-y art.
* *Magmin*: The bread-and-butter enemy of this level, it acts like a goblin with higher stats.  But, instead of growing every X turns, the Magmin *gains* 3HP whenever it gets burned.
* *Hellhound*: Like any other standard enemy, but with more HP and even more damage (5!).  They spawn in a "sleeping" state, and only wake up and start sliding when they are damaged in some way.
* *Dragon*: The strongest enemy in the game.  Spawns with 30 HP and deals 10 damage.  Moves only one square per turn, and every 3 turns, shoots fire in all 4 directions, burning anything in the fire's path.
* *Skeleton*: Same as in the dungeon.  We literally only added them so that they could be fodder for the bigger, tougher enemies in this area, especially the dragon.

I've written before about how we try to create interesting interactions between our different enemy types.  Here are some of the interactions you'll see in the volcano level:

* Lava slimes grow into big lava slimes very quickly because of their elevated starting HP.
* All enemies except the skeleton are fire-resistant.  This means they will take damage when burned, but they won't remain on fire for the following 3 turns the way you and the skeletons will.
* Dragon fire wakes the hellhounds.
* Dragon fire and lava pits *buff* the Magmin instead of damaging it.  So the dragon fire becomes an interesting, dynamic ability that damages most things, but also strengthens some of your enemies.
* Dragons deal huge amounts of damage, so sometimes the best way to clear other tiles is to keep the dragon alive and slide it towards other enemies you need to clear.

Again, the goal is to create engaging, thoughtful gameplay without mentally overloading the player.  Each of these abilities is simple enough on its own, but they become complex and interesting together.  We want to create a mixture of enemies that ends up being more than the sum of its parts.

![Animated GIF of a dragon tile shooting fire](/rs-devlog/5-volcano/dragon-fire.gif "Depending on the tile it hits, dragon fire does more than deal damage.")

## Tile Status Effects

A huge part of implementing volcano enemies was figuring out how "burn" was going to work.  I knew I wanted this logic to be a part of a bigger system of status effects, because we also want the player to be able to freeze and shock enemies using magic scrolls.  I was able to settle on a solution that involves using a new `StatusEffectManager` class that each tile has an instance of.  Here's how it works:

* Each tile has an instance of the `StatusEffectManager`.
* This manager keeps track of 3 different statuses: frozen, burnt, and shocked.  (Burnt is the only one implemented so far.)
* This manager also exposes the activity of a given status via computed properties.  So in my code I can check if a tile is already burning before I decide whether to re-apply the burn status, like so: `if (tile.status.isBurning) ...`
* Each tile has a function for applying the status effect, so I can override it for different tile types.  For example, the Magmin never takes damage on `burn()`, and never gets the burn status applied to it.  Instead, it gains HP!
* Each status effect has a special `onApply` function I can override, so I can, for example, play a "fire-burst" sound effect and animation when a tile starts burning.
* The status effect manager gets updated once at the end of every single turn.  Each of the 3 different statuses handle their own `onTurnEnd` logic.  At the very least, this logic counts down once per turn, and removes itself from the tile's active statuses once the countdown is over.  For example, after 3 turns, burning tiles will become "extinguished."
* Each status is in charge of its own draw logic, and has access to its parent tile.  For the burn status, this means the status can keep track of the tile's draw position and draw a little flame over the top of the tile art.  The same logic will be used for ice and shock effects once I implement those.

![Animated GIF of tile on fire](/rs-devlog/5-volcano/burn-status.gif "The system beneath this burn status is extensible enough that I'll be able to reuse it for the 'frozen' and 'shock' status effects that are in development.")

## Audio

If finishing the game's enemies gave it a spine, then audio gave it lifeblood.  The game feels so much more alive now that everything gives you juicy aural feedback!  A few hours of work (and some money spent on stock sfx packs) resulted in a hugely satisfying upgrade to our game's experience.

[Adding audio to a LibGDX game is fairly straightforward](https://github.com/libgdx/libgdx/wiki/Sound-effects), so I won't bore you with specifics.  However, I did run into an interesting problem that was specific to Rogueslide, and the solution turned out to be fairly simple.

In Rogueslide, there can be lots of the same enemy on the board at one time.  When you slide, all their attacks resolve at the exact same time.  In the original game jam version of Rogueslide, we had 2 or 3 different sound files for when tiles collided, and we just chose one of the 3 whenever a tile attacked another tile.  

This caused a problem: when the same sfx file was played for multiple enemy attacks, it caused an unpleasant sort of "overload" as the same file was played on top of itself a few times all at once.

Here are a few things I did to make audio implementation easy:
1. Make sure I have a large variety of sounds, and to have at least 2-3 version of each sound.
2. Make sure sound effects can only be played from exactly one place in the code.
3. Store a private `Set` of sound filenames on the `AssetManager` class.
4. Expose a method for "queueing" sound effects.
5. Expose another method for playing all queued sounds, and then clearing out the queue.

Numbers 1 and 2 are important for obvious reasons.  1) I wanted to have a lot of variety in my sound effects to avoid overlapping duplicate sounds in the first place, and also to simply make the game's audio more colorful and varying. And 2) I wanted to make really quick adjustments to the "master" volume of sound effects.  This made audio normalization (music vs sfx in this case) a lot easier, since I'm just tweaking one number for the whole codebase.  Also, the player has an "SFX Volume" slider in the options menu, and I wanted to make it easy to update the master SFX volume in the code based on changes to that value.

Those are things I would want to do for almost any game I work on, probably.  So that leaves us with numbers 3-5 to deal with the death-overlap issue I had in the game jam version of Rogueslide.

While tiles are sliding around and attacking and colliding, they notify the asset manager that it needs to play a certain sound.  For example, a goblin will want to play one of the 3-4 "blunt-attack" sound effects whenever it hits a tile, but the player will want to play one of the "slash-attack" sound effects. In other games I've made, that looks like this:

```kotlin
// in Player.kt
MyGame.assets.playSound("slash-attack-${MathUtils.random(1-3)}.wav")

// in Goblin.kt
MyGame.assets.playSound("blunt-attack-${MathUtils.random(1-4)}.wav")
```

They just pick one of the 3 or 4 different sound files for that particular sound and play it.

But in RogueSlide, I'm having the tiles queue up all the sound effects:

```kotlin
// in Player.kt
RogueSlide.assets.queueSound("slash-attack-${MathUtils.random(1-3)}.wav")

// in Goblin.kt
RogueSlide.assets.queueSound("blunt-attack-${MathUtils.random(1-4)}.wav")
```

It looks almost exactly the same, so there isn't any extra effort from within these tile classes.  The only extra step is to have the board play all the queued sounds at the very end of the each turn:

```kotlin
// in Board.kt
RogueSlide.assets.playQueuedSounds()
```

Super easy.  As I mentioned above, it works by adding sound filenames to an internal set within `AssetManager`.  Since it's a set, it will never have two of the same filenames.  `playQueuedSounds()` just plays a sound effect for each sound effect filename in the set, and then clears the set.

## Desktop Build Pipeline

Time to get dev-opsy.  The last big thing that happened this week was I added a CI pipeline to the GitLab repo for the project.  As a result, whenever I push version tags to the GitLab repo, GitLab automatically builds an executable for each desktop platform: Mac, Linux, and Windows.  This makes it incredibly easy for me to make changes, run a build, and send my team a link to download the latest build and playtest.

If you want to build a LibGDX game to run batteries-included on desktop platforms, then you need to bundle the JDK with your game.  You don't want the user to click on your game and then get a popup saying "this application requires Java!" or whatever.  For bundling, [LibGDX has this wonderful tool called packr](https://github.com/libgdx/packr).  Check out their GitHub page too see how it works.

So here's what my build pipeline does, starting from an `openjdk:8-jdk` docker image:
1. Installs Android (because for some reason this has to happen or gradle fails, even when I'm not touching the Android build???)
2. `wget`s the latest stable jar of packr [from the download link here](https://github.com/libgdx/packr/releases/tag/4.0.0).
3. For each of the 3 platforms (Mac/Win/Linux): a) Grabs the respective JDK for the platform from [Azul](https://www.azul.com/downloads/zulu-community/). b) Runs packr right there in the CI environment, bundling with the JDK from Azul using a configuration `json` file from the project's root directory. c) Zips the result into a downloadable artifact.
4. Cleans up by deleting the JDKs and by deleting packr.

That's about it!  There are a few more details, so if you want the full rundown checkout [this GitLab snippet](https://gitlab.com/-/snippets/2107849).  Feel free to copy this for your own projects.

## More to come!

Sorry for the late post this week.  Everything is coming together rapidly, and I'm excited about the momentum.  Follow along for more updates!  Until next time, happy coding.

