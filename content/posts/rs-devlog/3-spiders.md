---
title: "Make-it Monday: Rogueslide Devlog #3"
date: 2021-04-05
draft: false
summary: "This week, I'll cover how I'm creating enemy tiles with special abilities by talking about the spider enemy."
tags: ["rogueslide", "devlog", "make it monday"] 
---

Rogueslide development went a little slower than usual this week.  This is mostly due to the amount of effort and time I put into [my last post](/posts/entity-component-systems/), but also has to do with the holiday weekend, and some other family commitments that popped up.

That said, this will be a much shorter post than normal.

## This week in Rogueslide: Spiders!

Introducing the latest addition to the forest area of Rogueslide: Spiders!

![Animated GIF of rogueslide gameplay with spider enemy](/rs-devlog/3-spiders/spider-tile.gif "Spiders leave behind sticky webs for other tiles to get trapped in.")

In Rogueslide, we aim to create interesting and unique enemies without making the gameplay overly-complex.  We want spiders to feel like spiders, and dragons like dragons, but we don't want you to have to hurt your brain to really get what's going on each turn.  The gameplay mechanics should feel deep enough to be engaging, but simple enough to be casual. It's a fine line to walk, and I've found that games which walk that line well end up being very addicting.

With that in mind, there are a small handful of variables/stats that each tile has -- hp, attack damage, mobility, etc. -- that we can tweak.  In addition, we can add unique and interesting behaviors, but only add them in predictable places each turn -- pre-slide, post-slide, on death, etc.  This way, the player is (hopefully) never overwhelmed by tons of nuanced interactions in the game, yet there's enough of that nuance to keep things fresh.

## Hooking In

[In my devlog update from last week](/posts/rs-devlog/2-enemies/) I talked a little bit about tile "hook" methods.  These are empty-bodied methods that every tile has, and they allow for me to easily add custom behavior to different tile types.  The `onPostSlide` hook was put to good use when creating the spider.

The spider tile utilizes both stat tweaks *and* some special custom behavior to make it feel like a spider.  Our primary goal with the spider was we wanted it to leave behind webs. So I overrode the `onPostSlide` hook to get it to leave behind a `WebTile` object whenever it moves (as long as the previous space is still open).  Also, we playtested a little with having the spider slide all the way across the board, and leave a web on each tile it slid over, but that cluttered up the board way too quickly, so we changed its `baseMove` stat to 1.

In addition, we wanted the spider to be dangerous but lightweight.  So the spider does 2 damage when it hits other tiles (as opposed to the goblin's 1 damage), but it only has 3 HP.

![Animated GIF of spider enemy leaving webs and attacking the player tile](/rs-devlog/3-spiders/spider-attack.gif "Spiders don't have much HP, and they only slide one space per turn. But, they do 2 damange instead of 1. They also have a scratchy attack animation to differentiate their attacks visually.")

## Who's In Charge of What?

The last challenge in implementing the spider was figuring out how the webs should work.  

We wanted webs to be different from stone tiles in that we didn't want to have other tiles attack them to clear them.  Instead, a tile should slide *onto* a web space, and the web should disappear from underneath it.  This hopefully gives the feel that the tiles are sliding *through* the webs and getting stuck, rather than attacking them like they would any other tile.

But the implementation for this behavior was a little tricky. Currently each turn goes like this:

1. All tiles resolve their pre-slide logic.
2. All tiles slide until they can't slide no mo'.
3. All tiles resolve their `onCollide` logic with the tile in front of themselves as the collision target.
4. All tiles resolve their post-slide logic.

Webs don't slide at all, so they will never resolve any `onCollide` logic because they're never bumping into anything.  But other tiles bump into webs, and webs need to let those other tiles move into their space when that happens.  

I figured it would be easier for a web tile to say "Hey, you just hit me?  Ok, I'll remove myself from the board and you can take my spot."  As opposed to having everything else that could possibly hit a web tile say "Oh, are you a web tile?  Ok, let me remove you from the board and I'll move in to your spot."

In more formal terms, I don't want to couple the spiderweb's collision logic to every other tile's collide function. I want some of this:

```kotlin
class WebTile(...) {
    // ...

    fun onHit(otherTile: BaseTile) {
        this.removeFromGame()
        otherTile.moveTo(this.x, this.y)
    }
}
```

and less of this:

```kotlin
class EnemyTile(...) {
    fun onCollide(other: BaseTile) {
        if (...) {
            //...
        } else if (other is WebTile) {
            this.moveTo(other.x, other.y)
            other.removeFromGame()
        }
    }
}

class PlayerTile(...) {
    fun onCollide(other: BaseTile) {
        if (...) {
            //...
        } else if (other is WebTile) {
            this.moveTo(other.x, other.y)
            other.removeFromGame()
        }
    }
}

class ItemTile(...) {
    fun onCollide(other: BaseTile) {
        if (...) {
            //...
        } else if (other is WebTile) {
            this.moveTo(other.x, other.y)
            other.removeFromGame()
        }
    }
}
```

If we forget about the web tile for a minute and try to generalize this, the fundamental problem becomes this: who should own the collision handler? The answer is it depends. Sometimes I want the sliding tile to handle it (like when attacking), other times I want the obstructing tile to handle it (like with the spiderweb).  

My solution here was to split up collision handling into two separate functions in the `BaseTile` class: `onCollide(nextTile)` and `onSlideCheck(prevTile)`.  I'm not 100% sold on the naming, but the idea is that `onCollide` is called when I want the sliding (attacking) tile to handle the collision, and `onSlideCheck` is called when I want the obstructing tile to handle the collision.

An added bonus (esp. in the case of the spider web) is that I can call `onSlideCheck` while I'm incrementally sliding a tile one space at a time.  This makes the "slide me over the top of the spiderweb and replace the spiderweb" part of this problem easier.

If we zoom in on step 2 of each turn, it now looks like this:

1. Pre-slide
2. Slide each tile as far as it will slide.
    * For each tile, slide it one space at a time.
    * Each step, check if there is an obstructing tile.
    * If there is, check if anything special should happen here by doing `obstructingTile.onSlideCheck(slidingTile)`
3. `onCollide` collision logic, after every tile has slid to its proper place.

This logic and its implementation are subject to change, like everything else I write about Rogueslide here on the blog.  But it seems like a good solution for now.

## See You Next Week

Thanks so much for tuning in, and stay tuned for more updates!  Until then, stay away from spiders.

![Image of game over screen with spider tile](/rs-devlog/3-spiders/spider-death.png "Do spiders have names?  In Rogueslide they do.")