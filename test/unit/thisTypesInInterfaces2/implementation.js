/*
 interface Container<T> {
 value: T
 }

 export module module {
 function foo(): Container<string>;
 }*/

module.exports = {
    foo: function () {
        return {
            value: 123 // <- Not a string
        }
    }
};