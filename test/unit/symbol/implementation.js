/*declare namespace module {
    function foo(): symbol;
    function bar(): symbol;
}*/
module.exports = {
    foo: function () {
        return Symbol("foo")
    },
    bar: function () {
        return "bar"; // <- Not a symbol.
    }
};