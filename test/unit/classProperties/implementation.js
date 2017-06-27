/*declare class Class {
    constructor(a: number);
    constructor(a: string);

    static sta: number;
    inst: boolean;

    // Checking that only constructors are seen as constructors.
    static foo(a: boolean): void;
    foo(a: boolean): void;
}

export module module {
    function isCalled(a: typeof Class): true;
    function hasTwoConstructors(a: typeof Class): true;
    function hasStatic(a: typeof Class): true;
    function hasInst(a: typeof Class): true;
    function staticIsNotInstance(a: typeof Class): true;
    function instanceIsNotStatic(a: typeof Class): true;
    function doesntHaveBooleanConstructor(a: typeof Class): true;
}*/


module.exports = {
    isCalled: function (cla) {
        return false;
    },
    hasTwoConstructors: function (cla) {
        try {
            var foo = new cla(123);
            var foo = new cla("123");
            return true;
        } catch (e) {
            return false;
        }
    },
    doesntHaveBooleanConstructor: function (cla) {
        try {
            var foo = new cla(true);
            return false;
        } catch (e) {
            return true;
        }
    },
    hasStatic: function (cla) {
        return typeof cla.sta === "number";
    },
    hasInst: function (cla) {
        return typeof new cla(123).inst === "boolean";
    },
    staticIsNotInstance: function (cla) {
        return !("sta" in (new cla(123)));
    },
    instanceIsNotStatic: function (cla) {
        return !("inst" in cla);
    }
};