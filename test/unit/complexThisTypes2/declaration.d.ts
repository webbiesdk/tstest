// Type definitions for Pixi.js 4.1
// Project: https://github.com/pixijs/pixi.js/tree/dev
// Definitions by: clark-stevenson <https://github.com/pixijs/pixi-typescript>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

declare module module {
    class BasePrepare<UploadHookSource>{
        add(): this;
    }
    class CanvasPrepare extends BasePrepare<CanvasPrepare> {
        myMarker: true;
    }
}
