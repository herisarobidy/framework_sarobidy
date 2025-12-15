package mg.framework.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.util.regex.Pattern;


public class ControllerRegistry {
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<HandlerMethod>>> exactRoutes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<HandlerMethod>>> patternRoutes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

    public void register(HandlerMethod handler) {
        String path = handler.getPath();
        String httpMethod = handler.getHttpMethod();
        if (path.contains("{")) {
            ConcurrentHashMap<String, List<HandlerMethod>> inner = patternRoutes.computeIfAbsent(path, k -> new ConcurrentHashMap<>());
            List<HandlerMethod> list = inner.computeIfAbsent(httpMethod, k -> Collections.synchronizedList(new ArrayList<>()));
            list.add(handler);
            String regex = convertToRegex(path);
            compiledPatterns.put(path, Pattern.compile(regex));
        } else {
            ConcurrentHashMap<String, List<HandlerMethod>> inner = exactRoutes.computeIfAbsent(path, k -> new ConcurrentHashMap<>());
            List<HandlerMethod> list = inner.computeIfAbsent(httpMethod, k -> Collections.synchronizedList(new ArrayList<>()));
            list.add(handler);
        }
    }

    private String convertToRegex(String pattern) {
        return "^" + pattern.replaceAll("\\{[^}]+\\}", "([^/]+)") + "$";
    }

    public List<HandlerMethod> findExact(String path, String httpMethod) {
        ConcurrentHashMap<String, List<HandlerMethod>> inner = exactRoutes.get(path);
        if (inner == null) return Collections.emptyList();
        List<HandlerMethod> list = inner.get(httpMethod);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public List<HandlerMethod> findMatching(String path, String httpMethod) {
        List<HandlerMethod> exact = findExact(path, httpMethod);
        if (!exact.isEmpty()) return exact;

        for (Map.Entry<String, ConcurrentHashMap<String, List<HandlerMethod>>> entry : patternRoutes.entrySet()) {
            String pattern = entry.getKey();
            Pattern compiled = compiledPatterns.get(pattern);
            if (compiled != null && compiled.matcher(path).matches()) {
                ConcurrentHashMap<String, List<HandlerMethod>> inner = entry.getValue();
                List<HandlerMethod> list = inner.get(httpMethod);
                if (list != null && !list.isEmpty()) {
                    synchronized (list) {
                        return new ArrayList<>(list);
                    }
                }
                // If no specific method, try "ALL" for backward compatibility
                List<HandlerMethod> allList = inner.get("ALL");
                if (allList != null) {
                    synchronized (allList) {
                        return new ArrayList<>(allList);
                    }
                }
            }
        }

        // If no pattern matched for specific method, try exact "ALL"
        List<HandlerMethod> allExact = findExact(path, "ALL");
        if (!allExact.isEmpty()) return allExact;

        return Collections.emptyList();
    }

    public Map<String, Map<String, List<HandlerMethod>>> getExactRoutesSnapshot() {
        Map<String, Map<String, List<HandlerMethod>>> snap = new HashMap<>();
        for (Map.Entry<String, ConcurrentHashMap<String, List<HandlerMethod>>> e : exactRoutes.entrySet()) {
            Map<String, List<HandlerMethod>> innerSnap = new HashMap<>();
            for (Map.Entry<String, List<HandlerMethod>> innerE : e.getValue().entrySet()) {
                List<HandlerMethod> v = innerE.getValue();
                synchronized (v) {
                    innerSnap.put(innerE.getKey(), new ArrayList<>(v));
                }
            }
            snap.put(e.getKey(), innerSnap);
        }
        return snap;
    }

    public Pattern getCompiledPattern(String pattern) {
        return compiledPatterns.get(pattern);
    }
}
