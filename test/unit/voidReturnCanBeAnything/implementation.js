/*
export module module {
    function foo() : void; // <- actually any. 
    function bar(): boolean | void; // <- Not any.
}*/


module.exports = {
    foo: function () {
        return "any"; 
    },
    bar: function () {
        return "any"; 
    }
};