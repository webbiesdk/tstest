export module module {
    function twice(
        a: number | string,
        b: (a: string) => string
    ): string;
}