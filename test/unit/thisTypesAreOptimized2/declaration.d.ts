declare namespace module {
    export class Geometry {
        faceVertexUvs: Vector2[][][];
        clone(): this;
    }
    export class Vector2 {
        clone(): this;
        distanceToManhattan(v: Vector2): number;
    }
    export class DodecahedronGeometry extends Geometry {}
}