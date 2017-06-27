interface Selection<GElement> {
    specialMarker: true;
}

export module module {
    function brush(): (group: Selection<SVGGElement>) => void;
}