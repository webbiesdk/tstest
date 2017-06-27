/*

export module module {
    function foo() : CustomArray;

    interface CustomArray extends Array<string>{
        marker: true;
    }

    function bar(c: CustomArray): true;
}
 */


module.exports = {
    foo: function () {
        var result = ["string", "foo"];
        result.marker = true;
        return result;
    },
    bar: function (c) {
        return c.marker && c instanceof Array;
    }
};