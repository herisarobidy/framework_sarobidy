package mg.framework.init;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import mg.framework.registry.ControllerRegistry;
import mg.framework.scan.ClasspathScanner;

@WebListener
public class FrameworkInitializer implements ServletContextListener {
    public static final String REGISTRY_ATTR = "mg.framework.registry";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        ControllerRegistry registry = new ControllerRegistry();
        ClasspathScanner.scanAndRegister("", registry);
        ctx.setAttribute(REGISTRY_ATTR, registry);
        java.util.Map<String, java.util.Map<String, java.util.List<mg.framework.registry.HandlerMethod>>> snap = registry.getExactRoutesSnapshot();
        ctx.log("FrameworkInitializer: registered ControllerRegistry with " + snap.size() + " exact route paths");
        try {
            ctx.log("FrameworkInitializer: route listings (path -> method -> controller#method):");
            for (java.util.Map.Entry<String, java.util.Map<String, java.util.List<mg.framework.registry.HandlerMethod>>> e : snap.entrySet()) {
                String path = e.getKey();
                java.util.Map<String, java.util.List<mg.framework.registry.HandlerMethod>> inner = e.getValue();
                if (inner.isEmpty()) {
                    ctx.log("  " + path + " -> (no methods)");
                } else {
                    for (java.util.Map.Entry<String, java.util.List<mg.framework.registry.HandlerMethod>> innerE : inner.entrySet()) {
                        String method = innerE.getKey();
                        java.util.List<mg.framework.registry.HandlerMethod> list = innerE.getValue();
                        for (mg.framework.registry.HandlerMethod h : list) {
                            String line = String.format("  %s [%s] -> %s#%s -> %s", path, method, h.getControllerClass().getName(), h.getMethod().getName(), h.getReturnType().getSimpleName());
                            ctx.log(line);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            ctx.log("FrameworkInitializer: error while printing routes: " + t.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
