
module.exports = {
    foo: function () {
        if (Math.random() > 0.95) {
            return 1; // <- Returns normally, that is wrong!
        }
        var foo;
        foo.foo(); // <- Crashes
    }
};