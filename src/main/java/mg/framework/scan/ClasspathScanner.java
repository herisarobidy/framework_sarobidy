package mg.framework.scan;

import mg.framework.annotations.Controlleur;
import mg.framework.annotations.HandleURL;
import mg.framework.annotations.GetMapping;
import mg.framework.annotations.PostMapping;
import mg.framework.registry.HandlerMethod;
import mg.framework.registry.ControllerRegistry;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.util.Set;

public class ClasspathScanner {
    public static void scanAndRegister(String packageRoot, ControllerRegistry registry) {
        if (packageRoot == null) packageRoot = "";
        try {
            Reflections reflections;
            if ("/".equals(packageRoot)) packageRoot = "";
            if (packageRoot.isEmpty()) {
                java.util.List<java.net.URL> urls = new java.util.ArrayList<>();
                try {
                    java.util.Collection<java.net.URL> cp = ClasspathHelper.forJavaClassPath();
                    if (cp != null) urls.addAll(cp);
                } catch (Throwable ignore) {}
                try {
                    ClassLoader ctxCL = Thread.currentThread().getContextClassLoader();
                    java.util.Collection<java.net.URL> clUrls = ClasspathHelper.forClassLoader(ctxCL);
                    if (clUrls != null) urls.addAll(clUrls);
                } catch (Throwable ignore) {}
                try {
                    ClassLoader thisCL = ClasspathScanner.class.getClassLoader();
                    java.util.Collection<java.net.URL> thisUrls = ClasspathHelper.forClassLoader(thisCL);
                    if (thisUrls != null) urls.addAll(thisUrls);
                } catch (Throwable ignore) {}

                java.util.LinkedHashSet<java.net.URL> set = new java.util.LinkedHashSet<>(urls);
                java.util.List<java.net.URL> finalUrls = new java.util.ArrayList<>(set);

                ConfigurationBuilder cfg = new ConfigurationBuilder()
                        .setUrls(finalUrls)
                        .setScanners(Scanners.TypesAnnotated, Scanners.MethodsAnnotated);
                reflections = new Reflections(cfg);

                System.out.println("ClasspathScanner: scanning " + finalUrls.size() + " URLs for annotations (packageRoot=''") ;
                for (int i = 0; i < Math.min(finalUrls.size(), 20); i++) {
                    System.out.println("  URL[" + i + "]=" + finalUrls.get(i));
                }
            } else {
                reflections = new Reflections(packageRoot, Scanners.TypesAnnotated, Scanners.MethodsAnnotated);
                System.out.println("ClasspathScanner: scanning package '" + packageRoot + "'");
            }
            
            Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controlleur.class);
            System.out.println("ClasspathScanner: found " + controllers.size() + " controller classes (packageRoot='" + packageRoot + "')");
            for (Class<?> ctrl : controllers) {
                Method[] methods = ctrl.getDeclaredMethods();
                for (Method m : methods) {
                    if (m.isAnnotationPresent(HandleURL.class)) {
                        HandleURL h = m.getAnnotation(HandleURL.class);
                        String path = h.value();
                        HandlerMethod handler = new HandlerMethod(ctrl, m, path, "ALL");
                        registry.register(handler);
                    } else if (m.isAnnotationPresent(GetMapping.class)) {
                        GetMapping g = m.getAnnotation(GetMapping.class);
                        String path = g.value();
                        HandlerMethod handler = new HandlerMethod(ctrl, m, path, "GET");
                        registry.register(handler);
                    } else if (m.isAnnotationPresent(PostMapping.class)) {
                        PostMapping p = m.getAnnotation(PostMapping.class);
                        String path = p.value();
                        HandlerMethod handler = new HandlerMethod(ctrl, m, path, "POST");
                        registry.register(handler);
                    }
                }
            }
            } catch (Throwable t) {
                System.out.println("ClasspathScanner: error during scan: " + t.getMessage());
                t.printStackTrace(System.out);
            }
    }
}
