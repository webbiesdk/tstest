package dk.webbies.tajscheck.util;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 19-10-2016.
 */
public class HashSetMultiMap<K, T> implements MultiMap<K, T> {
    private Map<K, Set<T>> map = new HashMap<>();

    @Override
    public void put(K key, T value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            map.put(key, new HashSet<T>(Collections.singletonList(value)));
        }
    }

    @Override
    public Collection<T> get(K key) {
        if (map.containsKey(key)) {
            return Collections.unmodifiableCollection(map.get(key));
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<T> remove(K key) {
        Set<T> result = map.remove(key);
        if (result != null) {
            return result;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public Map<K, Collection<T>> asMap() {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void putAll(K key, Collection<T> value) {
        for (T t : value) {
            put(key, t);
        }
    }

    @Override
    public void putAll(MultiMap<K, T> map) {
        for (Map.Entry<K, Collection<T>> entry : map.asMap().entrySet()) {
            putAll(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }


    public static <K, T> Collector<? super Map.Entry<K, Collection<T>>, MultiMap<K, T>, MultiMap<K, T>> collector() {
        return new Collector<Map.Entry<K, Collection<T>>, MultiMap<K, T>, MultiMap<K, T>>() {
            @Override
            public Supplier<MultiMap<K, T>> supplier() {
                return HashSetMultiMap::new;
            }

            @Override
            public BiConsumer<MultiMap<K, T>, Map.Entry<K, Collection<T>>> accumulator() {
                return (map, entry) -> map.putAll(entry.getKey(), entry.getValue());
            }

            @Override
            public BinaryOperator<MultiMap<K, T>> combiner() {
                return (map1, map2) -> {
                    HashSetMultiMap<K, T> result = new HashSetMultiMap<>();
                    result.putAll(map1);
                    result.putAll(map2);
                    return result;
                };
            }

            @Override
            public Function<MultiMap<K, T>, MultiMap<K, T>> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.emptySet();
            }
        };
    }

    public boolean contains(K key, T value) {
        return get(key).contains(value);
    }
}
