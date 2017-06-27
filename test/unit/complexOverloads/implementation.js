/*
interface IPipelineFunction {
     (a: symbol): 0;
     (a: number): 1;
     (a: string): 2;
     (a: number, b: string): 3;
     (a: boolean): 4;
     (a: boolean, b: number): 5;
     (a: boolean, b: number, c: number): 6;
     (a: boolean, b: number, c: boolean): 7;
}
 */


module.exports = {
    run: function (callback) {
        switch (Math.random() * 8 | 0) {
            case 0:
                return callback(Symbol("foo")) == 0;
            case 1:
                return callback(123) == 1;
            case 2:
                return callback("string") == 2;
            case 3:
                return callback(123, "str") == 3;
            case 4:
                return callback(true) == 4;
            case 5:
                return callback(false, 123) == 5;
            case 6:
                return callback(true, 123, 321) == 6;
            case 7:
                return callback(true, 123, false) == 7;
            default:
                return false; // SHOULD NEVER HAPPEN.
        }
    }
};