/*
export module module {
    function extend<A, B>(obj: A, src: B): A & B;
}*/

module.exports = {
    extend: function (obj, src) {
        for (key in src) {
            obj[key] = src[key];
        }

        if (Object.keys(obj) <= 1) {
            return false;
        }

        return obj;
    }
};