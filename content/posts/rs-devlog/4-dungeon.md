---
title: "Make-it Monday: Rogueslide Devlog #4"
date: 2021-04-12
draft: false
summary: "This week in Rogueslide: the dungeon! Also, how I make enemy spawn frequencies editable with a code-free developer UI."
tags: ["rogueslide", "devlog", "make it monday", "ui", "kotlin"]
---

Rogueslide is still in the "make stuff" section of its development lifecycle, meaning my work is mostly spent adding enemy tiles these days.  However, most of these enemies were already implemented before the massive [UI refactor](/posts/rs-devlog/1-modals).  This means progress has been (mostly) very quick in adding all those tiles, because I'm solving problems that I've already solved before.

As a result, in one weekend I was able to add enough enemies to populate the entire second area of the game: the dungeon!

## This week in Rogueslide: The Dungeon

Rogueslide's main game mode takes you through three areas, each with 3 floors: the forest, the dungeon, and the volcano.  In the previous weeks, I've focused on developing tiles for the forest area.  With [the spider enemy](/posts/rs-devlog/3-spiders) completed, I've been able to move on to the dungeon.

![Animated GIF of dungeon level in rogueslide](/rs-devlog/4-dungeon/dungeon.gif "🎵 Spooky scary skeletons send shivers down your spine. 🎵")

If the goblin was the core enemy of the forest, the dungeon's bread-and-butter enemy type is the skeleton.  It's fitting -- what's a good dungeon crawler without a skeleton enemy?

## Raising The Dead

Skeletons in Rogueslide come in two flavors: normal and archer (ranged).  I'll talk more about the archers below.

The main gimmick of the skeleton tile (normal and archer) is that when it dies, it doesn't just disappear like the other tiles do.  It turns into a "pile of bones" tile that just sits on the board.  

These pile-o'-bones tiles don't slide anywhere, and so they can't hurt other enemies. They just sit in their place like rock tiles. Enemies can damage and clear them, and so can you. But if nothing clears out the pile-o'-bones within 5 turns, it'll reanimate into a skeleton with full health.

![Animated GIF of skeleton dying and turning into a pile of bones](/rs-devlog/4-dungeon/skelly-death.gif "It wouldn't be a proper dungeon without piles of bones that reanimate on their own.")

![Animated GIF of skeleton bone pile returning to full health as a skeleton](/rs-devlog/4-dungeon/skelly-reanimate.gif "IT'S ALIVE.")

If you've been following this devlog, then you're aware of how I'm using hooks to override core enemy behavior by "plugging in" to certain tile events.  And you can probably guess which hook I'm using here.  Here's some simplified code:

```kotlin
// in the skelly class
override fun onDie() {
  val bones = BonePileTile()
  board.placeTile(bones, this.tx, this.ty)
  screen.effects.fadeEffect("dust", centerX, centerY)
  super.onDie()
}
```

When the skelly dies it simply creates a pile-o'-bones.  The bone pile's `baseMove` stat is 0, so it will never slide, and its `baseAttack` is also 0, so it would never hurt anything even if it did slide.  Its HP is 3, and when it dies, it just disappears like a normal enemy tile.  But it has a special timer that I update every turn using the `onPreSlide` hook:

```kotlin
// in the bone pile tile class
private var countdown = 5

override fun onPreSlide(direction: Direction) {
    countdown--
    super.onPreSlide(direction)
}

override fun onPostSlide(direction: Direction) {
    if (countdown <= 0) {
        removeFromGame()
        val skelly = SkeletonTile(screen, skellyName)
        board.placeTile(skelly, this.tx, this.ty)
        screen.effects.fadeEffect("dust", centerX, centerY)
    }
    super.onPostSlide(direction)
}
```

Why not just use `onPostSlide` to decrement the counter *and* check if it's done?  Because I don't want the `onPostSlide` logic to run and decrement the counter on the same turn that this tile spawns.  

## Arrows For Archers

The archer enemies behave a bit differently than most tiles.  First of all, they don't slide like most other tiles -- they only move one space at a time, like the spider.  Second, they have a ranged attack in the form of an arrow.

This ranged arrow attack presented a few special problems:
1. We need to scan all tile positions "in front of" the skeleton to see who to attack, instead of just waiting for a collision to happen.
2. We need to fire an actual arrow projectile so the player can visually read what's happening.
3. We need to wait for that arrow to "hit" the target before we resolve the attack, otherwise the "-1" damage UI on the tile will show up early, and things will look weird.
4. We need to fire an arrow projectile even if the attack doesn't resolve.  For example, the skeleton still fires its arrow at rocks, but the rock isn't damaged (enemies don't damage rocks unless they are "massive", like the cyclops).

To solve these problems:
1. I overrode the `onCollide` method in the skeleton so that it never handles any attack/damage logic there, like most tiles do.  This is because the `onCollide` hook only happens when a tile actually bumps into another tile, and the skeleton archer will often be firing an arrow at something multiple tiles away.
2. I overrode the `onPostSlide` method to scan for the archer's target.
3. I treated the arrows like any other poolable effects by including a special `ProjectileEffect` class in my `EffectManager`. (I'll do a post soon on my custom `EffectManager` class that I always use for animated particles.)  Effects are just visuals, and they are completely unaware of attack/damage logic. This means that even if there's no tile targeted by the skeleton, I can still fire a `ProjectileEffect` and just have it harmlessly stick into the "wall" on the edge of the game board.
4. I used [higher-order functions](/posts/directional-foreach) to add an optional `callback` to the `ProjectileEffect`, and within that callback is where the attack/damage logic resolves, if a tile was targeted.

Here's the end result:

![Animated GIF of skeleton tile gameplay](/rs-devlog/4-dungeon/skelly-arrow.gif "The arrow still flies into the rock and wall, but the skelly only resolves an attack and deals damage when it fires the arrow at the player (or another enemy).")

## Custom Dev-Tools: Spawn Frequencies

If the whole board was full of only one predictable enemy, or lots of enemies with different stats that all behave the same, then gameplay would feel pretty bland.  This is why we design each area of Rogueslide to have one straightforward enemy, and then we design around that enemy with some other more complex enemies.  

Here's an example: Skeletons are a bit easy to handle on their own, but their bone piles clutter up the board and slow you down.  If that was your only enemy type, clearing the board would feel tedious.  So we have a cyclops tile that makes this situation more dynamic.  The cyclops has double the health of a skeleton, and it deals a whopping 3 damage (equal to your starting HP) to anything and everything in its way.  However the cyclops only moves 1 tile at a time, so it's easy enough to avoid it as long as you don't let yourself get cornered.

This leads to interesting gameplay decisions in the dungeon area.  While playtesting, I found myself corralling cyclops tiles, using their strength to my advantage to clear out the board.  It felt fun and engaging to work to line up the skeletons all in a row, slide them into each other until they were all bone piles, and then have the cyclops stomp out each bone pile in a single turn.  It also felt engaging and fun when my plan backfired, and I found myself surrounded by enemies -- one of which being the cyclops that I had preserved to do my bone-crushing.

[As I mentioned last week](/posts/rs-devlog/3-spiders), the guiding design philosophy is to keep it simple enough to feel casual, but complex enough to be engaging.  We want enough of the weird enemies to make the game spicy, but not so many that you can't make gameplay decisions somewhat intuitively.

You can imagine that walking this fine line requires lots of playtesting, and lots of iteration when it comes to the spawn frequencies of each tile. And that iteration can move really quickly when our designer doesn't have to tweak hardcoded values in our code and rebuild the project each time a tweak needs to be made.

So with that in mind, I created a simple "developer tools" interface in the title screen's option menu.  This interface shows a slider UI widget for each enemy that could possibly spawn in the given area.  That slider then determines the percentage chance that the given enemy will spawn on that floor within that area.

![Animated GIF of spawn-frequency sliders in the dev tools menu.](/rs-devlog/4-dungeon/spawn-sliders.gif "It's not the prettiest UI in the game, but it doesn't have to be.")

Behind the scenes, I have a super-simple data class that holds these spawn values in a list.  The developer tools menu loads up an instance of this data class in memory, and every time a slider value changes, it saves the modified instance to a json file stored in my assets folder.  The `Spawner` class that loads enemies each turn just looks at those values to determine which enemy to spawn.

```kotlin
// spawn data classes for json serialization
@Serializable
class TileSpawnProbability(val classname: String, var probability: Float = 0f) {
    val name: String
        get() = classname.split(".").last()
}

@Serializable
class SpawnData {
    val forestProbs: List<List<TileSpawnProbability>> = listOf()
    val dungeonProbs: List<List<TileSpawnProbability>> = listOf()
    val volcanoProbs: List<List<TileSpawnProbability>> = listOf()
}
```

```kotlin
// dev tools menu code that generically sets up sliders for each spawn probability 
fun initSpawnSlider(prob: TileSpawnProbability) {
    val slider = Slider(0f, 100f, 1f, false, skin, "options-slider")
    slider.value = prob.probability
    val sliderLabel = Label("${prob.name}: ${prob.probability}%", skin, "white-64")
    slider.addListener(object: ChangeListener() {
        override fun changed(event: ChangeEvent?, actor: Actor?) {
            if (event != null) {
                prob.probability = slider.value
                SpawnService.save()
                sliderLabel.setText("${prob.name}: ${prob.probability}%")
            }
        }
    })
    spawnTable.add(sliderLabel).left().padLeft(tileWidth / 2f).padBottom(settingsLabelPad)
    spawnTable.row().padBottom(settingsLinePad)
    spawnTable.add(slider).growX().padLeft(tileWidth / 2f).padRight(tileWidth / 2f)
    spawnTable.row()
}

fun initAreaSpawnSliders(areaName: String, areaLevelProbs: List<List<TileSpawnProbability>>) {
    areaLevelProbs.forEachIndexed { i, level ->
        val levelLabel = Label("$areaName ${i + 1}", skin, "menuTitle")
        spawnTable.add(levelLabel).center().padBottom(settingsLinePad)
        spawnTable.row()
        level.forEach { tileProb -> initSpawnSlider(tileProb) }
    }
}

initAreaSpawnSliders("Forest", SpawnService.data.forestProbs)
initAreaSpawnSliders("Dungeon", SpawnService.data.dungeonProbs)
initAreaSpawnSliders("Volcano", SpawnService.data.volcanoProbs)
```

```javascript
// serialized json in assets folder
{
  "forestProbs": [
    [
      {
        "classname": "com.beepyeah.rogueslide.tiles.GoblinTile",
        "probability": 75.0
      },
      {
        "classname": "com.beepyeah.rogueslide.tiles.VineTile",
        "probability": 2.0
      },
      {
        "classname": "com.beepyeah.rogueslide.tiles.SpiderTile",
        "probability": 7.0
      },
      {
        "classname": "com.beepyeah.rogueslide.tiles.SlimeTile",
        "probability": 21.0
      }
    ],
  ],
  "dungeonProbs": [...],
  "volcanoProbs": [...]
}
```

That's the idea in a nutshell.  There's a bit more going on here (kotlinx serialization, a singleton spawner service, and even some class reflection), but I'll spare the details.

## See Ya

That wraps up this week's Rogueslide update!  Once again, thanks so much for joining me. Stick around for more updates, and as always, happy coding.