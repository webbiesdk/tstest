// Type definitions for Pixi.js 4.1
// Project: https://github.com/pixijs/pixi.js/tree/dev
// Definitions by: clark-stevenson <https://github.com/pixijs/pixi-typescript>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

declare module module {
    export module prepare {
        export abstract class BasePrepare<UploadHookSource>{
            constructor();
            register(): this;
            add(): this;
        }
        export class CanvasPrepare extends BasePrepare<CanvasPrepare> {
            constructor();
        }
        export class WebGLPrepare extends BasePrepare<WebGLPrepare> {
            constructor();
        }

    }
}