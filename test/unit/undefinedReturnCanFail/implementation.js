/*
export module module {
    function foo() : undefined; // <- must be undefined
    function bar() : void; // <- can be anything
}*/

module.exports = {
    foo: function () {
        return "any"; 
    },
    bar: function () {
        return "any"; 
    }
};