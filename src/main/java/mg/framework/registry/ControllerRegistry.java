package mg.framework.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.*;


public class ControllerRegistry {
    private final ConcurrentHashMap<String, List<HandlerMethod>> exactRoutes = new ConcurrentHashMap<>();

    public void register(HandlerMethod handler) {
        List<HandlerMethod> list = exactRoutes.computeIfAbsent(handler.getPath(), k -> Collections.synchronizedList(new ArrayList<>()));
        list.add(handler);
    }

    public List<HandlerMethod> findExact(String path) {
        List<HandlerMethod> list = exactRoutes.get(path);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public Map<String, List<HandlerMethod>> getExactRoutesSnapshot() {
        Map<String, List<HandlerMethod>> snap = new HashMap<>();
        for (Map.Entry<String, List<HandlerMethod>> e : exactRoutes.entrySet()) {
            List<HandlerMethod> v = e.getValue();
            synchronized (v) {
                snap.put(e.getKey(), new ArrayList<>(v));
            }
        }
        return snap;
    }
}
