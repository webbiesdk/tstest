
module.exports = {
    foo: function () {
        // Date | {foo: boolean} | (() => boolean)
        switch ((Math.random() * 3) | 0) {
            case 0: return new Date();
            case 1: return {bar: true};
            case 2: return function () {
                return "string"; // <- not a boolean!
            }
        }
    }
};