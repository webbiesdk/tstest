/*declare namespace module {
    function foo(arg: Foo): "foo";

    class Foo {
        stuff(): "foo";
    }
}*/


module.exports = {
    foo: function (Foo) {
        return "blallala"; // <- definitely wrong.
    }
};