package mg.framework.registry;

import java.lang.reflect.Method;

public final class HandlerMethod {
    private final Class<?> controllerClass;
    private final Method method;
    private final String path;
    private final Class<?> returnType;
    private final String httpMethod;

    public HandlerMethod(Class<?> controllerClass, Method method, String path, String httpMethod) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.path = path;
        this.returnType = method.getReturnType();
        this.httpMethod = httpMethod;
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

    public Class<?> getReturnType() {
        return returnType;
    }

    public String getHttpMethod() {
        return httpMethod;
    }
}
