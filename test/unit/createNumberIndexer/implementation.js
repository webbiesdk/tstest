
module.exports = {
    id: function (a) {
        return a; // <- Correct
    },
    foo: function (obj) {
        obj[123] = "foo"; // <- Breaks the signature
        return obj;
    }
};