/*interface Bar {
    value: true;
}
declare module module {
    function gen(): Bar; // always returns something incorrect, therefore all tests only run if this method ever gets overwritten.
}*/

module.exports = {
    gen: function () {
        return {
            value: false
        }
    }
};