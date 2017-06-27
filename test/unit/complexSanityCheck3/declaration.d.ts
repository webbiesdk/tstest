// Example is from box2dWeb
declare module module {
    export class b2Shape {
        public Set(other: b2Shape): void;
    }
    export class b2CircleShape extends b2Shape {
        public GetRadius(): void;
        public Set(other: b2CircleShape): void;
    }
}
