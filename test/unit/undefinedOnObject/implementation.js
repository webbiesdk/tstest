/*
export module module {
    function foo(a: Foo) : true
}
interface Foo {
    a?: true;
    b?: true;
    c?: true;
}*/

module.exports = {
    foo: function (a) {
        for (var key in a) {
            if (!a[key]) {
                return false;
            }
        }
        return true;
    }
};