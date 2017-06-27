declare var module: MemoizedFunction;

interface MemoizedFunction extends Function {
    marker: true;
}