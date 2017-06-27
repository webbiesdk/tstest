/*

type impossible = number & string & boolean & 1 & false;

export module module {
    function foo(c: () => impossible) : true
}
*/

var hasRun = false;

var module = {
    foo: function (c) {
        if (hasRun) {
            c();
        } else {
            hasRun = true;
        }
        return false;
    }
};