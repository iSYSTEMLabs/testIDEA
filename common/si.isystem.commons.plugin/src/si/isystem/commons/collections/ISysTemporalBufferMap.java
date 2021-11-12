package si.isystem.commons.collections;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A map holding only the specified number of least recently added/changed key-value pairs.
 * 
 * @author iztokv
 *
 * @param <K>
 * @param <V>
 */
public class ISysTemporalBufferMap<K, V> implements ITemporalBufferMap<K, V> {

    private int m_maxSize;
    private final Map<K, V> m_map = new HashMap<>();
    private final Deque<K> m_keyFifo = new ArrayDeque<>();
    
    public ISysTemporalBufferMap() {
        this(100);
    }

    public ISysTemporalBufferMap(int maxSize) {
        m_maxSize = maxSize;
    }

    public int getMaxSize() {
        return m_maxSize;
    }

    public void setMaxSize(int maxSize) {
        if (maxSize >= 0) {
            m_maxSize = maxSize;
        }
        else {
            m_maxSize = 0;
        }
        // Remove excess entries
        while (m_keyFifo.size() > m_maxSize) {
            K key = m_keyFifo.removeLast();
            m_map.remove(key);
        }
    }

    @Override
    public void put(K key, V value) {
        // If already exists put to front of queue
        if (m_map.containsKey(key)) {
            m_keyFifo.remove(key);
        }
        m_keyFifo.addFirst(key);
        m_map.put(key, value);
        
        while (m_map.size() > m_maxSize){
            K removedKey = m_keyFifo.removeLast();
            m_map.remove(removedKey);
        }
    }
    
    @Override
    public int size() {
        return m_map.size();
    }

    @Override
    public boolean contains(K key) {
        return m_map.containsKey(key);
    }

    @Override
    public V get(K key) {
        return m_map.get(key);
    }
    
    @Override
    public K getOldestKey() {
        return m_keyFifo.getLast();
    }
    
    @Override
    public V getOldestValue() {
        K oldestKey = m_keyFifo.getLast();
        return m_map.get(oldestKey);
    }
    
    @Override
    public K getNewestKey() {
        return m_keyFifo.getFirst();
    }

    @Override
    public V getNewestValue() {
        K newestKey = m_keyFifo.getFirst();
        return m_map.get(newestKey);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(30*m_map.size() + 50);
                
        sb.append("TEMP BUFF KEYS [").append(m_keyFifo.size()).append("]: ");
        for (K k : m_keyFifo) {
            sb.append(k).append(", ");
        }
        sb.append("\n");
        
        sb.append("MAP: [").append(m_map.size()).append("]: ");
        for (Entry<K, V> e : m_map.entrySet()) {
            sb.append("['").append(e.getKey()).append("' -> '").append(e.getValue()).append("'], ");
        }
        sb.append("\n");

        return sb.toString();
    }
}
