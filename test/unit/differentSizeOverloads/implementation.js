/*
interface IPipelineFunction {
    (token:string):number;
    (token:string, tokenIndex:number):string;
    (token:string, tokenIndex:number, tokens:string[]):boolean;
}
 */


module.exports = {
    run: function (callback) {
        switch (Math.random() * 3 | 0) {
            case 0:
                return typeof callback("string") === "number";
            case 1:
                return typeof callback("string", 123) === "string";
            case 2:
                return typeof callback("string", 123, ["foo", "bar"]) === "boolean";
            default:
                return false; // SHOULD NEVER HAPPEN.
        }
    }
};