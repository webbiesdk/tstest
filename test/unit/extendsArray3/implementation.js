
/*
export module module {
    interface CustomArray extends Array<string[]> {
        marker: true;
    }

    function bar(): CustomArray;
}
*/

module.exports = {
    bar: function () {
        var result = [["test", "foo"]];
        result.marker = true;
        result.push([123]); // <- WRONG
        return result;
    }
};