declare namespace module {
    export class b2Fixture {
        public GetDensity(): number;
    }

    export class b2World {
        public RayCastAll(): b2Fixture[];
    }
}