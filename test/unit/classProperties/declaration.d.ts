export module module {
    function isCalled(a: typeof Class): true;
    function hasTwoConstructors(a: typeof Class): true;
    function hasStatic(a: typeof Class): true;
    function hasInst(a: typeof Class): true;
    function staticIsNotInstance(a: typeof Class): true;
    function instanceIsNotStatic(a: typeof Class): true;
    function doesntHaveBooleanConstructor(a: typeof Class): true;

    class Class {
        constructor(a: number);
        constructor(a: string);

        static sta: number;
        inst: boolean;

        // Checking that only constructors are seen as constructors.
        static foo(a: boolean): void;
        foo(a: boolean): void;
    }

}