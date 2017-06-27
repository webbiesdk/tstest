
module.exports = {
    foo: function(callback) {
        return callback({
            value: 123 // <- not a string.
        });
    }
};