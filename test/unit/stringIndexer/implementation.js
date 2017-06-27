
module.exports = {
    foo: function () {
        return {
            foo: 1,
            bar: 4,
            baz: 1,
            quz: "blah" // <- not a number
        };
    },
    bar: function () {
        return {
            foo: [true, false],
            bar: [false]
        };
    }
};