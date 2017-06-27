module.exports = {
    foo: function () {
        return {
            value: {
                foo: 123 // <- Not a string.
            }
        }
    }
};