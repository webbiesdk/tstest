declare module Box2D {
    export class b2Shape {
        setSibling(other: b2Shape): void;
        getSibling(): b2Shape;
    }
    export class b2CircleShape extends b2Shape {
        setSibling(other: b2CircleShape): void;
        getSibling(): b2CircleShape;
        getRadius(): number;
    }
}
