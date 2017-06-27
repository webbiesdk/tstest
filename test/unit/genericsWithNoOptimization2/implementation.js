/*
 interface Container<T> {
 value: T,
 flagObj: {
 flag: boolean
 }
 }

 export module module {
 function foo(): Container<string>;
 function bar(): Container<number>;
 }
 */

module.exports = {
    foo: function () {
        return {
            value: "string",
            flagObj: {
                flag: 123 // <- Not a boolean
            }
        }
    },
    bar: function () {
        return {
            value: 123,
            flagObj: {
                flag: 123 // <- Not a boolean
            }
        }
    }
};