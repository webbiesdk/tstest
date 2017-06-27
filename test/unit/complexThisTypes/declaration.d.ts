interface HeadAndTail<T> {
    head: T,
    tail: T
}

declare namespace module {
    class Foo {
        getHeadAndTail(): HeadAndTail<this>;
    }

    class Bar extends Foo {
        bar(): string;
    }
}
