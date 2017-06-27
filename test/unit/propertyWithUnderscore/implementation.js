
module.exports = {
    foo: function (a) {
        if (a.__foo) {
            return {
                __bar: true
            };
        }
        return Object.keys(a)[0];
    }
};