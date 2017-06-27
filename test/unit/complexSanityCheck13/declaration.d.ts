declare module module {
    export interface Vector {}
    export class Vector2 implements Vector {}
    export class Curve<T extends Vector> {}
    export class CurvePath<T extends Vector> extends Curve<T> {
        getPoint(t: number): T;
    }
    export class Path extends CurvePath<Vector2> {}
}