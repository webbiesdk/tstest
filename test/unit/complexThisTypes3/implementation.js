/*
declare namespace module {
    export class Layer {
        bindPopup(): this;
    }
    export class LayerGroup extends Layer {
        addLayer(): void;
        eachLayer(fn: (layer: Layer) => void): this;
    }
}*/

var Layer = function () {
    this.bindPopup = function () {
        return this;
    }
};
var layerGroup = function () {
    Layer.call(this);
    this.addLayer = function () {
        return this;
    };
    this.eachLayer = function (fn) {
        fn(new Layer());
        return this;
    }
};
module.exports = {
    Layer: Layer,
    LayerGroup: layerGroup
};