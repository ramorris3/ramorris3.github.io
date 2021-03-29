---
title: "Make-it Monday: Rogueslide Devlog #2"
date: 2021-03-29
draft: false
summary: "For Make-it Monday this week, I'm covering some of the content I added to Rogueslide now that the big UI refactor is done."
tags: ["rogueslide", "devlog", "make it monday", "skins"]
---

The last 10% the the game is 90% of the work.  How many devlogs have opened with that line?  It's embarrassingly cliche, but here we are.

With Rogueslide nearing 2 years of off-and-on development, it sure feels like we're in the that "last 10%." The game knows what it is, almost all of its systems are in place, most of the art is done, and importantly the entire game is designed on paper.  

Now that so much of the game's vision is decided on, it's just a matter of executing on that vision by plugging everything in and duct-taping it all together.  And adding a little bit of sparkly polish on top of all that tape.

Let's really quickly go over all of this week's major updates to the code.

## This week in Rogueslide: UI Fixes, Unique Areas, and More Tiles

Now that the UI refactor is done, and the game's systems are very soundly in place, my focus is in making sure all the art and functionality is plugged back in.  This week, that means I spent a lot of my time in [Skin Composer](https://github.com/raeleus/skin-composer), a tool for building LibGDX UIs. 

![Skin Composer screenshot with progress bar from volcano level.](/rs-devlog/2-enemies/skin-composer.png "Skin Composer lets you create LibGDX skins and preview them in action.")

![Screenshot of all the drawables in the Rogueslide skin in Skin Composer.](/rs-devlog/2-enemies/drawables.png "Even a small game like Rogueslide utilizes lots of drawables in its UI.")

### A HUD for each area

The main gameplay screen of Rogueslide was programmed to load the UI widgets based on the current area. So rather than creating a separate class for each area's UI, I just made a single UI class that works based on asset naming conventions.  

Here's an example: In the forest, the HUD widgets are all green.  But our other two areas (dungeon and volcano) have different color palettes, so a green HUD doesn't make sense there.

![Screenshot of forest level in Rogueslide.](/rs-devlog/2-enemies/forest.png)

![Screenshot of dungeon level in Rogueslide.](/rs-devlog/2-enemies/dungeon.png)

![Screenshot of volcano level in Rogueslide.](/rs-devlog/2-enemies/volcano.png)

The gameplay HUD is made up of text labels, some slider/progress bars, and a couple drawable textures (the door icon above the "turns" slider, and the scroll button).

I'm sure I could've loaded the widget assets into Skin Composer and then tinted them to the colors we wanted, but it didn't take me that long to just load 3 copies of each widget.

> **Pro Tip**: The ["hue adjustment" tool in Aseprite](https://community.aseprite.org/t/is-it-possible-to-change-hue/104) was a life-saver for making UI widgets match the color of their respective areas without having to bug our artist for more assets!

When creating the widgets in SkinComposer, I just made sure that the widgets followed a specific naming convention which includes the area name: `<area name>-turns-bar`, or `<area name>-scroll-btn`, etc.  This means that the HUD code doesn't need to behave differently depending on which area's UI it's loading.  It just looks at the game state to grab the area name and then interpolates that in the asset name string:

```kotlin
// area is an enum, so we convert it to a lowercase string
private val areaName = RogueSlide.gameState.area.name.toLowerCase()
private val turnBar = ProgressBar(
  min = 0f, 
  max = 100f, 
  stepSize = 1f, 
  vertical = false, 
  skin = skin, 
  styleName = "$areaName-turns"
)
```

### Hooks for Boards

I also wanted to create specific features for each area's "board," but I wanted to be able to do this without re-writing similar code.  At the same time, it's not as simple as the HUD, where we are literally loading up the exact same widgets but with different styles.

In an effort to make our Board object flexible without becoming redundant, I used a small-games pattern that I've used for many projects: "hooks".

"Hooks" are what I call the methods that I put in base classes that are only optionally overridden.  This isn't a mind-blowing practice--it's just basic object-oriented design.  But I think it's worth mentioning because it's often a good way to think about your game objects.

Now, before you turn your anti-object-oriented nose up at me, let me say this: Most of my game code ends up as a hybrid of lots of different paradigms, tweaked and fine-tuned to fit the needs of that specific project.  Remember that you're making a game above everything else, so just do whatever works for that game! If a pattern (or part of a pattern) serves that goal, then use it.  If not, don't worry about it.

Let's start by looking at the `Board` class as an example.  Originally, the `Board` was just a single class that loaded the background based on asset naming conventions (similar to what I mentioned above in the HUD section).  

But each board differs outside of just which assets are drawn on the screen.  For example, in the dungeon, we want to have animated torch objects along the walls.  In the volcano, we added an animated flowy lava effect around the borders of the board.  (Our artist Nathan came up with a really cool way to achieve this affect.  I'll have to go into detail in a future post.)

![Gif of volcano area with lava effect.](/rs-devlog/2-enemies/lava.gif "The volcano area's board has a special requirement: an animated outer border that looks like flowing lava")

Once those requirements arose, it was pretty trivial to just extract the "setup" logic into its own function, have it load the background tiles by default (shared logic no matter which area this board belongs to) and then override and extend that as needed for the dungeon and volcano.  In other words, I'm "hooking" into the setup of the board, and adding logic there as needed.

### Hooks for Tiles

A much more useful example of hooks are the hook methods on my tiles.  In Rogueslide, tiles behave differently, but their behavior diverges only at very specific places in the code.  For example, all tiles slide as much as they can, and all tiles collide with each other, but what do they do when they collide? What about after collisions have resolved?  Most tiles won't do something at the end of every turn, but some of them will, and I want to have a place for this logic to live.

In Rogueslide, the player tile and rock tiles don't do anything special after each turn.  But goblins gain HP every few turns, and vines gain HP every few turns *and* spawn baby vines every few turns, which in turn grow and make more vine babies.

![Animated gif of Rogueslide, with goblins and vines growing and spreading each turn](/rs-devlog/2-enemies/post-slide.gif "Goblins and vines are great examples of the need for a 'post-slide' hook in my BaseTile class.")

The `BaseTile` class has a `postSlide` method that the board calls on every tile at the very end of each turn.  The body of that `postSlide` method is empty in `BaseTile`, meaning when it gets called on most tiles, nothing will happen.  But in the `GoblinTile` class, I increment a counter after each turn, and when the counter reaches its limit, the goblin gains HP.  Similarly, when the vine's counter reaches its limit, it makes baby vines. More generally, whenever I create a new enemy that needs to do something at the end of each turn, it's easy for me to hook into that moment of gameplay and execute some behavior.

> Pitfall warning: always be aware of the tendency to over-generalize and get lost in the weeds. Don't worry about making hooks until you need them.

Now, this is *very* basic object-oriented programming, but I want to make it clear that I'm being deliberate about my vernacular here.  I've found that thinking in terms of hooks makes my life a lot easier when I'm designing gameplay because it helps me ask crucial questions about the behavior of my game objects.  

It's especially useful in a turn-based game like Rogueslide, where each game object's behaviors resolve in a specific sequence.  In other words, each turn goes through a set of phases, and hooks are just a way for each tile to plug into each phase of the turn.  

You can tell how I've broken up each turn in Rogueslide by looking at the `BaseTile` class's hooks:

* `onPreSlide`
* `onCollide`
* `onDamage`
* `onDie`
* `onKill`
* `onPostSlide`

And here are some practical examples of how I'm using these hooks:

* Goblins use `onPostSlide` to gain HP every 3 turns.
* Vines use `onPostSlide` to gain HP every 5 turns, and make baby vines.
* Slime enemies attack other tiles `onCollide`, but if the other tile is another slime, they combine into one bigger slime.
* The player uses `onCollide` to interact with different tiles depending on their type: attack an enemy, pick up a potion or scroll, or go down stairs to enter the next level.
* The player uses the `onKill` hook to gain exp whenever it delivers the final blow to a tile.
* Spiders leave a web tile behind them `onPostSlide`
* Dragons use `onPostSlide` to shoot fire in all 4 directions every few turns.
* Skeletons use `onDie` to leave behind a pile-o'-bones tile, which uses `onPostSlide` to reanimate back into a skeleton enemy after 3 turns.
* Skeleton archers use `onPreSlide` to determine which tile they will be firing an arrow at without actually sliding into that tile.

Even though this line of thinking is incredibly useful in a turn-based game with turn phases, you can still use the same thought process when designing the core game loop for real-time games, too.

## Safely Traversing "The Swamp"

To wrap things up, let's shift gears for a moment and talk about game development more generally.

As I said at the beginning of this article, Rogueslide feels so close to being done.  At this point, it's just a matter of trudging through [what Derek Yu calls "the Swamp"](https://www.derekyu.com/makegames/archetypes.html#inventor) -- the middle part of your game's development, where you have the vision of the game, and your main job is to just execute on that vision by simply making lots of stuff.

I've thought a lot about the swamp section of a game's development, because so many of my game prototypes have died slowly in that mid-section mire. 

Derek Yu has already written extensively on [finishing games](https://makegames.tumblr.com/post/1136623767/finishing-a-game).  I simply wouldn't do the topic justice if I were to summarize here what he's already said.  I'm also definitely not an expert on finishing games.  (I've only "released" a very small portion of all the games I've started.)

However, I want to take a minute to talk about some things I've discovered about the Swamp part of development specifically.  Hopefully if you follow my advice, you can avoid some of the pitfalls that have made this game's development go on longer (probably) than it should have.

### Scope It Down

This piece of advice comes first not only because it's the most important one, but because it feeds into all the other advice below in some way or another.

The basic idea is this: your game is going to evolve over the course of its development, and you want to make sure that evolution happens quickly and early-on.  

You'll spend time designing your game on paper before you actually set out to make it, but until you have a playable prototype, you're not going to know if it works.  If your game comes together exactly like you imagined, great!  But most likely, it won't.

When you inevitably run into design crossroads for your game, remove features instead of adding them.  Get at the very core of what your game is trying to be, and strip out everything else that doesn't add to that core.  And then strip out some of the features that _do_ add to that core, but are going to keep you from getting through the swamp for some reason or another.  

This is the privilege of working on little games as a solo/small team: You can get to the essence of what you're making, and just _crush_ the execution of that core idea without adding fluff to please investors.

### In General, Don't Generalize Too Much

I had an interesting realization about small game programming recently: In an ideal world, you will release your game and forget about it.  

This is so very different from working as an enterprise software developer.  In the professional software world, you're trying to write code that will be maintained and extended by hundreds of other developers over the next decade.  You want to make things flexible and general and readable and bullet-proof.  Your business requirements are constantly changing as the market shifts, and as new customers and clients come and go.

With small games, you are writing a single product.  You'll probably need to fix bugs and release patches, but ideally your product won't change too much once it's out there.

With this in mind, remember to write the code for you.  Just do what makes sense for you (and your team, if you're not solo), and for your game. Remember that you're writing a game above everything else.

If you're embarrassed of your code, who cares?  Most of your audience won't be going through your game's source code.  If they do, they shouldn't judge you for it as long as it delivers the game experience in the intended way.

If you get too focused on writing generalized systems, then you could waste months or years writing systems instead of writing a game.  And, when your game needs to pivot like I mentioned above, your systems will be outdated and dysfunctional anyway.

### "These are not the [swamps] you're looking for..."

If you have very low tolerance for swampiness, then use your jedi mind tricks to significantly cut down on swamp in the first place.

Maybe your brutal platformer game can go from 5 areas -- each with unique tileset assets and gameplay mechanics -- to a single area with a few more levels.  Or if you're working on an action game with dozens of levels, you can just finish up the game by getting rid of the level design requirement and making it an endless arcade game with a single "level" instead of dozens of levels.

This is basically a re-hash of "scope down" above, but it feels important to make note of it on its own. I think sometimes as developers we feel like we're selling ourselves short by compromising on scope and features.  The truth is this: a finished, polished, and released endless arcade game with a small swamp can be every bit as fun as a handcrafted, 100-level, story-driven platformer with miles and miles of swamp as far as the eye can see.  

Show yourself compassion when you make compromises on scope and content.  Remember that coming out of the swamp with a smaller game will pretty much always feel better than dying in the swamp with a bigger game.

### Become a Swamp Explorer

Game development is going to be a grind sometimes.  You'll almost never be able to scope a project down so much that the entire project is a joy to work on.  At the end of the day, you *will* need some amount of conditioning if you're going to be able to make it through the grindy bits.

If you know you've got a long, grindy section of development ahead, then learn how to get comfortable in that grind.  Make it a mindfulness practice.  Turn on some good music, grab a drink, and settle in for an hour.  Make sure to touch your project once a day, if possible.  

I remember reading that Maddy Thorson said of one of their early metroidvania projects that they treated it like a journal, and designed a new room every day based on their mood. 

Be accountable to someone.  Get used to writing or talking about what you worked on each week, and eventually you'll start to naturally break up big projects into manageable chunks of work without really thinking about it. (Why do you think I'm keeping this blog that no one reads??)

Do whatever you need to do to make the swamp meaningful and enjoyable, and you'll be done in no time.  What's more, your mindful time in the swamp will prepare you for longer and longer excursions with each project.  You'll build up a tolerance, and your games will naturally grow in scope over time.

## Until Next Week

Well I covered a lot more than I intended with this update, and I realize it was all over the place.  If you made it this far, thanks for sticking around.  If not, no hard feelings at all.

There are some small features and updates that I didn't get around to mentioning in this article.  Hopefully the more I do these weekly updates, the easier it will be for me to distill all the updates into more digestible/readable articles.

Our humble project is moving along each week, and we're excited with the progress we're seeing. We hope you're excited, too! 

Stick around for more updates.  In the meantime, stay safe, and happy coding.
