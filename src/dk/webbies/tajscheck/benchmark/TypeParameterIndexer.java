package dk.webbies.tajscheck.benchmark;

import dk.au.cs.casa.typescript.types.TypeParameterType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by erik1 on 30-01-2017.
 */
public class TypeParameterIndexer {
    private final boolean combineAllUnboundGenerics;
    private final Map<TypeParameterType, Integer> map = new HashMap<>();
    public static final String IS_UNSTRAINED_GENERIC_MARKER = "_isUnboundGeneric";

    TypeParameterIndexer(CheckOptions options) {
        this.combineAllUnboundGenerics = options.combineAllUnboundGenerics;
    }

    public String getMarkerField(TypeParameterType t) {
        if (combineAllUnboundGenerics) {
            return IS_UNSTRAINED_GENERIC_MARKER;
        }
        if (map.containsKey(t)) {
            return "_genericMarker" + map.get(t);
        } else {
            map.put(t, map.size());
            return getMarkerField(t);
        }
    }
}
