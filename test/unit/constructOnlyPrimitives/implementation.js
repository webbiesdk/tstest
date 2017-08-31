/*declare namespace module {
    function foo(foo: {composite: true}): true;
}*/

module.exports = {
    foo: function (foo) {
        return false; // <- Definitely wrong.
    }
};