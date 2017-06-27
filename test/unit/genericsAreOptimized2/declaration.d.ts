declare namespace module {
    export interface Matrix {}
    export class Matrix3 implements Matrix {
        multiplyVector3Array(a: any): any;
    }
    export interface Vector {}
    export class Vector3 implements Vector {
        setFromSpherical(): Matrix3;
    }
    export class Curve<T extends Vector> {
        getPoint(t: number): T;
    }
    export class CatmullRomCurve3 extends Curve<Vector3> {}
}