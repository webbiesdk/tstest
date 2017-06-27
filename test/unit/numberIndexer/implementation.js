
module.exports = {
    foo: function () {
        return {
            1: 1,
            3: 4,
            7: 1,
            10: "blah" // <- not a number
        };
    },
    bar: function () {
        return {
            1: [true, false],
            6: [false]
        };
    }
};