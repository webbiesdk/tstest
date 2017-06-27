declare module module {
    function time<T>(
        offset: number,
        k: (result: number) => T
    ): T
}