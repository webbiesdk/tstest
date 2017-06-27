package dk.webbies.tajscheck.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by erik1 on 19-10-2016.
 */
public interface MultiMap <K, T> {
    void put(K key, T value);

    Collection<T> get(K key);

    Collection<T> remove(K key);

    Set<K> keySet();

    boolean containsKey(K key);

    Map<K, Collection<T>> asMap();

    void putAll(K key, Collection<T> value);

    void putAll(MultiMap<K, T> map);

    int size();

    boolean isEmpty();
}
