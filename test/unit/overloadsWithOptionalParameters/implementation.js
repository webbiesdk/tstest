/*
interface IPipelineFunction {
    (a: number, b?: boolean): 0;
    (a: boolean, b?: number): 1;
    (a: string): 2;
    (a: string, b: string): 3;
}
 */

function assertEquals(a, b) {
    if (a !== b) {
        console.log()
    }
    return a === b;
}

module.exports = {
    run: function (callback) {
        switch (Math.random() * 6 | 0) {
            case 0:
                return assertEquals(callback(123), 0);
            case 1:
                return assertEquals(callback(123, true), 0);
            case 2:
                return assertEquals(callback(true), 1);
            case 3:
                return assertEquals(callback(false, 123), 1);
            case 4:
                return assertEquals(callback("string"), 2);
            case 5:
                return assertEquals(callback("string", "str"), 3);
            default:
                return false; // SHOULD NEVER HAPPEN.
        }
    }
};