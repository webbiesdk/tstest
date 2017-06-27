/*
export module module {
    function foo(a: {marker1 : true} & {marker2: true}) : false
}
 */
module.exports = {
    foo: function (a) {
        return a.marker1 && a.marker2; // <- Wrong, this returns true, it should return false.
    }
};