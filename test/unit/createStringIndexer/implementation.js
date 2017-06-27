
module.exports = {
    id: function (a) {
        return a; // <- Correct
    },
    foo: function (obj) {
        obj.foo = "foo"; // <- Breaks the signature
        return obj;
    }
};