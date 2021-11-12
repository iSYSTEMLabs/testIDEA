package si.isystem.commons.collections;

/**
 * Essentially a map that holds only the specified last number of unique key-value pairs.
 * 
 * @author iztokv
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface ITemporalBufferMap<K, V> {
    public int size();
    public boolean contains(K key);
    public V get(K key);
    public void put(K key, V value);
    
    public K getOldestKey();
    public V getOldestValue();

    public K getNewestKey();
    public V getNewestValue();
}
