var MinerGame = MinerGame || {};

// SPLASH SCREEN STATE //
MinerGame.splashState = function(){};

MinerGame.splashState.prototype = {
    create: function() {
        this.clock = 150;
        this.game.add.image(this.game.world.centerX - 400, this.game.world.centerY - 300, 'splash-screen');
    },
    update: function() {
        this.clock--;
        if (this.clock <= 0) {
            this.game.state.start('menu');
        }
    }
}