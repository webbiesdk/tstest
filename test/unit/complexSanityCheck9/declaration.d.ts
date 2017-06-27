// Example is from leaflet
declare namespace module {
    export interface FeatureGroup {
        setStyle(style: {}): void;
    }
    export interface GeoJSON extends FeatureGroup {
        setStyle(style: () => {}): void;
    }
    var foo: GeoJSON;
}