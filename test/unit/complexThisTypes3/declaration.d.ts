declare namespace module {
    export class Layer {
        bindPopup(): this;
    }
    export class LayerGroup extends Layer {
        eachLayer(fn: (layer: Layer) => void): this;
    }
}