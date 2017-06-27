
export module module {
    function using<R1, R2, R3>(executor: (transaction1: R1, transaction2: R2, transaction3: R3) => boolean): boolean;
}