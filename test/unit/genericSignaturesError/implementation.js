/*declare namespace MyModule {
    type Wlala<T> = (instance: T) => string;
    function createElement<T>(): Wlala<T>;
}*/

module.exports = {
    createElement: function () {
        return function(instance) {
            return 123; // <- not a string.
        }
    }
};