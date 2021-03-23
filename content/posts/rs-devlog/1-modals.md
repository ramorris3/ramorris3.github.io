---
title: "Make-it Monday: Rogueslide Devlog #1"
date: 2021-03-22T20:05:03-06:00
draft: true
summary: "This week for Make-it Monday I'm introducing my upcoming game Rogueslide.  I also cover some of the technical problems I encountered with the game's UI, and how I solved those problems."
tags: ["rogueslide", "devlog", "make it monday", "ui"]
---

Welcome to Make-it Monday, fellow game lovers! I'm excited to introduce this new article series where I talk about what I worked on the previous week. This will be a great place for me to hold myself accountable for the projects I'm working on, to articulate and distill the learning experiences I've had (which will help me internalize them), and (hopefully!) for you to learn a thing or two by following along.  Seems like a win, win, win.

For the first entry in this series, I'm thrilled to talk about my current project, [Rogueslide](https://store.steampowered.com/app/1443100/Rogueslide/).  Rogueslide is a sliding puzzle dungeon crawler in development for desktop and mobile.  It's got casual controls, but deep and strategic gameplay.  

The idea was originally conceived by the inimitable [Tom Brinton](https://twitter.com/brintown), a friend of mine who pitched it to me for the [LibGDX jam in August 2019](https://itch.io/jam/libgdx-jam-august-2019).  We created [a small version for the jam](https://itch.io/jam/libgdx-jam-august-2019/rate/477490) and decided the concept had legs. So we continued working on it, and eventually brought on Nate Thomson for art, and Christian Walter to help with code.

A year and a half later, and we're almost there.

![Rogueslide game jam gif with sliding tiles](/rs-devlog/1-modals/rs-jam-demo.gif "The jam version of Rogueslide was barebones with only a few tile types: the player, goblins, potions, and stones.")

![Rogueslide full version gif with sliding tiles](/rs-devlog/1-modals/rs-full-demo.gif "The full version has 3 unique areas with a handful of different enemies for each, plus items, upgrades, and player abilities.  Not to mention Nate's wonderful animated art.")

So much of my inspiration for code-specific articles for this blog has come from the development of this game.  It's very different from other games I've made.  It makes use of plenty of specific features and libraries from LibGDX that I normally don't touch. I've worked hard to develop some good patterns around these new tools, and hope to share some of that knowledge with you.

## This Week (more like month) in Rogueslide: Scene Graphs and UI

I've spent years in LibGDX, yet this is the first project where I've used Scene2D in earnest.  For those who are unfamiliar, [Scene2D](https://github.com/libgdx/libgdx/wiki/Scene2d) is an optional library within LibGDX that allows you to organize your game objects into a scene graph.  

The concept of a scene graph is certainly not new.  As an example, the popular open-source engine [Godot](https://godotengine.org/) is built entirely around this paradigm. It's a nice way to manage objects in your game.

The main idea is that all of the objects in your game belong somewhere in this hierarchy of objects, and any actions you apply to one node in the hierarchy will also affect all of that node's children.

![Illustration of scene graph with ui, player, enemies, etc.](/rs-devlog/1-modals/scene-graph-illustration.png "In a scene graph, everything is a node (even the scene itself).  Actions applied to one node affect all of that node's children.")

## Scene2D Features

LibGDX's Scene2D library is useful for many reasons, but here are the ones I'll focus on for today:

1. Rotation, scale, and translation, etc. of one node affects all that node's children.
2. Scene2D has a built-in actions system which makes it incredibly easy to manipulate nodes in the scene over time. (Bonus: it includes a stupid-easy API for interpolating those actions.)
3. Hit detection/input routing is handled completely by Scene2D.

I've never used it simply because in many ways it clashes with the Entity Component System (ECS) setup I've used for almost every 2D project for 5+ years. (I won't get into ECS's in this article, because soon I want to devote an entire article to my go-to setup.)  But for this project, I felt I really had no choice: Scene2D's features are valuable enough that I became determined to make it play nice with the rest of the project's code.

## Why Graphs?

So your game objects all fit in a graph. So what? This doesn't change anything if each object just worries about itself when it's time to update/draw.  Classic separation of concerns, right?

Well, let's say you have a pause menu in your game.  That menu will consist of a handful of options for the user to choose: "restart", "music volume", "quit to menu", "quit to desktop", etc. 

The main requirement for a pause menu is that it only pops up when the game is paused. Easy, just keep the pause menu's logic and children in the same place in your code, and draw all of them (or don't, when you don't want to) within a single `draw` method.

But hold on: you've added a ton of options to your pause menu, and your pause menu class is getting huge.  You break it out into a bunch of different entities, each with their own `draw` method.  Whew, ok, you can still contain everything in that one parent class's `draw` method, and everything is fine.

Let's get even fancier and say that each widget in the pause menu is its own entity, and each entity has components that determine its position, its update logic, its drawing properties, etc.  Now our concerns are so separate that we could get a job architecting microservices in Silicon Valley.

Ok, suddenly you have a new requirement: the pause menu is pretty jarring when it just instantaneously appears.  You want it to have a nice fade-in effect.  That's still fine... probably... just gotta track down all these separate entities and add my fancy `AlphaComponent` to them...

All right, now let's rotate the menu's widgets about the center of the menu.  And scale the menu and everything in it.  So now you're running around trying to track down all of those widgets, and make sure they know what the pivot point of rotation is, and how big they're supposed to be, and what opacity...

## Scene2D Nodes and Node Children

This is a very contrived example, but it's not a contrived problem.  Take a look at this pause menu's transition:

![Example of pause menu fade in Rogueslide](/rs-devlog/1-modals/pause-fade.gif "It's funny how much polish a simple fade effect can add to your game.")

This pause menu is made of a `Label` object and a handful of `TextButtons`.  And with a Scene2D graph, it became absolutely trivial to fade all of them in at once.  I just added all the buttons to the menu node and completely forgot about them.  When I want to fade the pause menu in, I do _exactly that_ in my code:

```kotlin
// pause menu is disabled, and its opacity is 0f
val actionSequence = Actions.sequence(Actions.show(), Actions.fadeIn(duration = 0.25f))
pauseMenu.addAction(actionSequence)
```

And that's it.  No cascading calls to all the widgets within my menu, no keeping track of all the children, and those childrens' children... My code becomes much more direct and concise.

## Scene2D Actions and Interpolation

So while I was writing the sample code for the pause menu fade-in above, I got butterflies in my tummy, and I started feeling all warm and fuzzy.  Because the actions system is definitely my favorite discovery of my Scene2D expedition.  Once I found it in the docs and began to understand how I could use it, I knew that I absolutely had to have it in my game code.

So let's revisit the fade-in example above.  If your requirements are simple enough, you could certainly get by without a hierarchy of actors.  But look at this game over modal in the Rogueslide UI:

![Example of Rogueslide modal that falls and bounces onto the screen](/rs-devlog/1-modals/game-over-fall.gif "If you want to translate every widget in this UI modal, then suddenly you have to manage tons of state over time.")

The widgets all need to move and position themselves relative to the parent model.  That means that to code something like this from scratch, the modal needs to track its children and update their state over time, or the children need to reference their parent and update their own state based on the parent's state.  

Even more, we're not just looking at moving in a linear fashion from off-screen to on-screen.  We accelerate the modal down from offscreen to it's target Y position, then make it bounce. That means you have to keep track of not only position over time, but you have to do that within 2-3 different states of movement.

The Scene2D action system not only gives us the benefit of applying the same action to a node and all of it's children, but it allows us to queue those actions sequentially, _and_ add interpolation to those actions.  Here's the code for that modal popping in and slamming down:

```kotlin
// start from top, drop in fast, then bounce a tiny bit (one tile-height's worth)
val dropSequence = Actions.sequence(
    Actions.show(),
    Actions.moveBy(0f, viewHeight),
    Actions.fadeIn(0f),
    Actions.moveBy(0f, -viewHeight, duration = 0.25f, Interpolation.slowFast),
    Actions.moveBy(0f, tileHeight / 2f),
    Actions.moveBy(0f, -tileHeight / 2f, duration = 0.25f, Interpolation.bounceOut)
)
gameOverMenu.addAction(dropSequence)
```

As a reminder, I've already set up the menu and added all its children to it, and completely forgotten about them.  When I want to move the `gameOverMenu`, I do _exactly_ that, and don't even have to think about any other nodes.

I highly encourage you to check out the [Scene2D wiki's section on actions](https://github.com/libgdx/libgdx/wiki/Scene2d#actions), and also [look at the different options available for interpolation](https://github.com/libgdx/libgdx/wiki/Interpolation). It's incredible how much more polished and smooth your game looks when you add some simple interpolation, and this API makes it stupid easy. 

## Scene2D Input Routing

The last benefit I mentioned near the top of this article was input routing. All this means is that each actor can say what should happen when it recieves an input event, and the Scene2D Stage will handle the rest.  This is really nice for a few reasons:

1. Major separation of concerns: anything that handles input in my game only handles its own input events.
2. Declarative code: instead of polling for input every frame, I get to declare what my widget does when it gets clicked and then forget about it.
3. Input routing: the input gets directed to only the actor that was touched, and not everything underneath it.

The first two kind of explain themselves, so let's focus on #3.  In Rogueslide, we don't have this functionality yet, but we're hoping to add tooltips to tiles.  These could show the tile's name and description, some stats, cheeky flavor text, etc., when the player touches the tiles.  That would make Grimsnack the Goblin an `InputProcessor` that overrides the `touchDown` method.

Well, in this screenshot, our pause menu is also an active `InputProcessor`:

![Image of pause menu overlapping a goblin tile in Rogueslide](/rs-devlog/1-modals/pause-menu-overlap.png "When we click on this menu item, we don't want Grimsnack's lore tooltip to also pop up.")

Since these are both nodes in the same scene graph, Scene2D will cleverly route the input to whichever node is on top in the scene first.  Also, depending on the return value of your `touchDown` input handler method, you can control whether Scene2D keeps traversing the scene graph to look for other actors that were hit.  This means you could short-circuit like we want to in the example above (only `retry` is touched, Grimsnack's tile is not), _or_ if you have a use case in your game where you need to handle input for a whole bunch of overlapping items all at once, you can do that too!

## Scene You Later

Thanks for listening!  If you made it this far, you have a high tolerance for ranting and geeking out.  And puns.  I'm excited about our game, and I'm very excited to share things I'm learning along the way.  

I hope it saves you time and pain in your own projects, and more importantly, I hope it makes your own game development projects more enjoyable in some way.  Your projects _should_ be enjoyable, because when everything is said and done, we make games because making games is fun.