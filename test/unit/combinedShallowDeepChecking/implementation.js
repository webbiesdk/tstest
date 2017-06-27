/*
export module module {
    function K(): void; // <- doesn't exist.
    function foo(): true; // <- returns false
}
*/

module.exports = {
    foo: function () {
        return false;
    }
};