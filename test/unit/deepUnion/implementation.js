
// function foo() : {foo: {bar: boolean}} | {foo: {bar: string}};
module.exports = {
    foo: function () {
        // Date | {foo: boolean} | (() => boolean)
        switch ((Math.random() * 2) | 0) {
            case 0: return {
                foo: {
                    bar: true
                }
            };
            case 1: return {
                foo: {
                    bar: 123
                }
            };
            default:
                throw new Error("Not reachable");
        }
    }
};