/*
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
}*/

function Geometry() {
    this.faceVertexUvs = [[[new Vector2(), new Vector2(), new Vector2()], [new Vector2(), new Vector2()]]];
    this.clone = function () {
        return this;
    }
}

function Vector2() {
    this.clone = function() {
        return this;
    }
    this.distanceToManhattan = function (v) {
        return "thisIsNotADistance"; // <- WRONG.
    }
}

function DodecahedronGeometry() {
    Geometry.call(this, arguments);
}

module.exports = {
    Geometry: Geometry,
    Vector2: Vector2,
    DodecahedronGeometry: DodecahedronGeometry
};