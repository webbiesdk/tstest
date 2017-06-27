/*declare class Foo {
    stuff(): "foo";
}

declare namespace module {
    function foo(arg: typeof Foo): "foo";
}*/
module.exports = {
    foo: function (Foo) {
        return new Foo().stuff() + "Bar"; // <- The last "Bar" is wrong!
    }
};