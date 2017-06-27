declare module module {
    // Should both be matched, assert that after 100 runs.
    function foo(x: true): true;
    function foo(x: false): true;

    // This just returns !!x.marker2.
    function bar(x: B): true;
    function bar(x: A): false;

    // This always returns false
    function baz(x: A): false;
    function baz(x: A): true;

    class A {
        marker1: true;
    }
    class B extends A {
        marker2: true;
    }
    class C extends A {
        marker3: true;
    }
}