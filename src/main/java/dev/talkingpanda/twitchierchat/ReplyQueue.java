package dev.talkingpanda.twitchierchat;
import java.util.LinkedHashMap;
import java.util.Map;


public class ReplyQueue<V> {

    private long keyCounter = 0;

    private final Map<Long, V> map = new LinkedHashMap<>(Config.getMaxHistory(), 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, V> eldest) {
            return size() > Config.getMaxHistory();
        }
    };

    public long add(V value) {
        long currentId = keyCounter++;
        map.put(currentId, value);
        return currentId;
    }

    public V get(long id) {
        return map.get(id);
    }
}
