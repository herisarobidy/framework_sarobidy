package mg.framework.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.util.regex.Pattern;


public class ControllerRegistry {
    private final ConcurrentHashMap<String, List<HandlerMethod>> exactRoutes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<HandlerMethod>> patternRoutes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

    public void register(HandlerMethod handler) {
        String path = handler.getPath();
        if (path.contains("{")) {
            List<HandlerMethod> list = patternRoutes.computeIfAbsent(path, k -> Collections.synchronizedList(new ArrayList<>()));
            list.add(handler);
            String regex = convertToRegex(path);
            compiledPatterns.put(path, Pattern.compile(regex));
        } else {
            List<HandlerMethod> list = exactRoutes.computeIfAbsent(path, k -> Collections.synchronizedList(new ArrayList<>()));
            list.add(handler);
        }
    }

    private String convertToRegex(String pattern) {
        return "^" + pattern.replaceAll("\\{[^}]+\\}", "([^/]+)") + "$";
    }

    public List<HandlerMethod> findExact(String path) {
        List<HandlerMethod> list = exactRoutes.get(path);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public List<HandlerMethod> findMatching(String path) {
        List<HandlerMethod> exact = findExact(path);
        if (!exact.isEmpty()) return exact;

        for (Map.Entry<String, List<HandlerMethod>> entry : patternRoutes.entrySet()) {
            String pattern = entry.getKey();
            Pattern compiled = compiledPatterns.get(pattern);
            if (compiled != null && compiled.matcher(path).matches()) {
                List<HandlerMethod> list = entry.getValue();
                synchronized (list) {
                    return new ArrayList<>(list);
                }
            }
        }
        return Collections.emptyList();
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

    public Map<String, List<HandlerMethod>> getPatternRoutesSnapshot() {
        Map<String, List<HandlerMethod>> snap = new HashMap<>();
        for (Map.Entry<String, List<HandlerMethod>> e : patternRoutes.entrySet()) {
            List<HandlerMethod> v = e.getValue();
            synchronized (v) {
                snap.put(e.getKey(), new ArrayList<>(v));
            }
        }
        return snap;
    }
}
