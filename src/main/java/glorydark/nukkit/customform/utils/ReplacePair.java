package glorydark.nukkit.customform.utils;

/**
 * @author glorydark
 */
public class ReplacePair<K, V> {

    private K key;
    private V value;

    public ReplacePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    // Getter和Setter方法
    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "KeyValuePair{key=" + key + ", value=" + value + "}";
    }
}
