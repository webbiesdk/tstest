
/*
export module module {
    interface CustomArray extends Array<string[]>{
        marker: true;
    }

    function bar(c : CustomArray): true;
}
*/

module.exports = {
    bar: function (c) {
        if (!c.marker) {
            return false;
        }
        if (typeof c.length !== "number") {
            return false;
        }
        if (c.length == 0) {
            return true;
        }
        for (var i = 0; i < c.length; i++) {
            var stringArr = c[i];
            if (typeof stringArr.length !== "number") {
                return false;
            }
            for (var j = 0; j < stringArr.length; j++) {
                if (typeof stringArr[j] !== "string") {
                    return false;
                }
            }
        }
        return true;
    }
};