package mg.framework.registry;

import java.lang.reflect.Method;

public final class HandlerMethod {
    private final Class<?> controllerClass;
    private final Method method;
    private final String path;

    public HandlerMethod(Class<?> controllerClass, Method method, String path) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.path = path;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
