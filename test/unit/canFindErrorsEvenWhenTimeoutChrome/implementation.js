/*

export module module {
    function foo() : true
}
*/

// The point of this is that it has an error, but also that it takes so long, that running the normal 10000 iterations would timeout. 
// So this is testing that even in that case, we still get a result.

var hasRun = false;

var module = {
    foo: function () {
        if (hasRun) {
            while (true) {
                console.log("Printing away, FOREVER!");
            }
        } else {
            hasRun = true;
        }
        return false;
    }
};