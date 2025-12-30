package mg.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import mg.framework.annotations.RequestParam;
import mg.framework.model.ModelView;


@WebServlet(name = "FrontServlet", urlPatterns = {"/"}, loadOnStartup = 1)
public class FrontServlet extends HttpServlet {
    private mg.framework.registry.ControllerRegistry registry;

    @Override
    public void init() throws ServletException {
        super.init();
        Object attr = getServletContext().getAttribute(mg.framework.init.FrameworkInitializer.REGISTRY_ATTR);
        if (attr instanceof mg.framework.registry.ControllerRegistry) {
            this.registry = (mg.framework.registry.ControllerRegistry) attr;
            getServletContext().log("FrontServlet : registry chargé avec " + this.registry.getExactRoutesSnapshot().size() + " routes exactes");
        } else {
            getServletContext().log("FrontServlet : aucun ControllerRegistry trouvé dans le ServletContext");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        service(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        service(request, response);
    }
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();       
        String resourcePath = requestURI.substring(contextPath.length());

        if (registry != null) {
            java.util.List<mg.framework.registry.HandlerMethod> handlers = registry.findMatching(resourcePath, request.getMethod());
            if (handlers != null && !handlers.isEmpty()) {
                for (mg.framework.registry.HandlerMethod h : handlers) {
                    try {
                        Object controllerInstance = h.getControllerClass().getDeclaredConstructor().newInstance();
                        String handlerPath = h.getPath();
                        Object[] args = new Object[0];
                        java.lang.reflect.Parameter[] params = h.getMethod().getParameters();
                        if (handlerPath.contains("{")) {
                            List<String> vars = extractVarNames(handlerPath);
                            Pattern compiled = registry.getCompiledPattern(handlerPath);
                            if (compiled != null) {
                                Matcher matcher = compiled.matcher(resourcePath);
                                args = new Object[params.length];

                                if (matcher.matches()) {
                                    for (int i = 0; i < params.length; i++) {
                                        String paramName = params[i].getName();
                                        int varIndex = vars.indexOf(paramName);
                                        if (varIndex != -1) {
                                            String value = matcher.group(varIndex + 1);
                                            args[i] = convertValue(value, params[i].getType());
                                        } else {
                                            RequestParam requestParam = params[i].getAnnotation(RequestParam.class);
                                            if (requestParam != null) {
                                                String reqParamName = requestParam.value().isEmpty() ? paramName : requestParam.value();
                                                int pathVarIndex = vars.indexOf(reqParamName);
                                                if (pathVarIndex != -1) {
                                                    String value = matcher.group(pathVarIndex + 1);
                                                    args[i] = convertValue(value, params[i].getType());
                                                } else {
                                                    String value = request.getParameter(reqParamName);
                                                    if (value != null) {
                                                        args[i] = convertValue(value, params[i].getType());
                                                    } else if (params[i].getType().isPrimitive()) {
                                                        args[i] = getDefaultValue(params[i].getType());
                                                    } else if (isMapStringObject(params[i])) {
                                                        args[i] = request.getParameterMap();
                                                    }
                                                }
                                            } else if (params[i].getType().isPrimitive()) {
                                                args[i] = getDefaultValue(params[i].getType());
                                            } else if (isMapStringObject(params[i])) {
                                                args[i] = request.getParameterMap();
                                            }
                                        }
                                    }
                                    // Lier les objets personnalisés
                                    Map<String, Object> customObjects = bindCustomObjects(params, request);
                                    for (int i = 0; i < params.length; i++) {
                                        if (args[i] == null && isCustomObject(params[i].getType())) {
                                            args[i] = customObjects.get(params[i].getName());
                                        }
                                    }
                                } 
                            }
                        } else {
                            args = new Object[params.length];
                            for (int i = 0; i < params.length; i++) {
                                String sourceName = params[i].getName();
                                RequestParam requestParam = params[i].getAnnotation(RequestParam.class);
                                if (requestParam != null && !requestParam.value().isEmpty()) {
                                    sourceName = requestParam.value();
                                }
                                String value = request.getParameter(sourceName);
                                if (value != null) {
                                    args[i] = convertValue(value, params[i].getType());
                                } else if (params[i].getType().isPrimitive()) {
                                    args[i] = getDefaultValue(params[i].getType());
                                } else if (isMapStringObject(params[i])) {
                                    args[i] = request.getParameterMap();
                                }
                            }
                            // Lier les objets personnalisés
                            Map<String, Object> customObjects = bindCustomObjects(params, request);
                            for (int i = 0; i < params.length; i++) {
                                if (args[i] == null && isCustomObject(params[i].getType())) {
                                    args[i] = customObjects.get(params[i].getName());
                                }
                            }
                        }
                        Object result = h.getMethod().invoke(controllerInstance, args);
                        if (result instanceof String) {
                            response.getWriter().println((String) result);
                        } else if (result instanceof ModelView) {
                            ModelView mv = (ModelView) result;
                            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/" + mv.getView());
                            if (dispatcher != null) {
                                for (java.util.Map.Entry<String, Object> entry : mv.getAttributes().entrySet()) {
                                    request.setAttribute(entry.getKey(), entry.getValue());
                                }
                                dispatcher.forward(request, response);
                            } else {
                                response.getWriter().println("Vue introuvable : " + mv.getView());
                            }
                        } else {
                            response.getWriter().println("Type de retour non pris en charge : " + result.getClass().getName());
                        }
                    } catch (Exception e) {
                        response.getWriter().println("Erreur lors de l'appel de la méthode : " + e.getMessage());
                    }
                }
                return;
            }
        }

        try {
            java.net.URL resource = getServletContext().getResource(resourcePath);
            if (resource != null) {
                if (resourcePath.endsWith(".jsp")) {
                    RequestDispatcher jspDispatcher = getServletContext().getRequestDispatcher(resourcePath);
                    if (jspDispatcher != null) {
                        jspDispatcher.forward(request, response);
                        return;
                    }
                }
                RequestDispatcher defaultServlet = getServletContext().getNamedDispatcher("default");
                if (defaultServlet != null) {
                    defaultServlet.forward(request, response);
                    return;
                }
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de la vérification de la ressource: " + resourcePath, e);
        }

        response.getWriter().println("Ressource non trouvée: " + resourcePath);

    }

    private List<String> extractVarNames(String pattern) {
        List<String> vars = new ArrayList<>();
        Pattern p = Pattern.compile("\\{([^}]+)\\}");
        Matcher m = p.matcher(pattern);
        while (m.find()) {
            vars.add(m.group(1));
        }
        return vars;
    }

    private Object convertValue(String value, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == char.class || type == Character.class) {
            return value.length() > 0 ? value.charAt(0) : '\0';
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        } else if (type == String.class) {
            return value;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private Map<String, Object> bindCustomObjects(java.lang.reflect.Parameter[] methodParams, HttpServletRequest request) {
        Map<String, Object> boundObjects = new java.util.HashMap<>();
        java.util.Map<String, String> bindingErrors = new java.util.HashMap<>();
        Map<String, String[]> paramMap = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String paramName = entry.getKey();
            if (!paramName.contains(".")) continue; // seulement les paramètres de la forme object.*
            String[] tokens = paramName.split("\\.");
            if (tokens.length < 2) continue; // doit être au minimum object.prop
            String rootName = tokens[0];
            String[] path = java.util.Arrays.copyOfRange(tokens, 1, tokens.length); // chemin restant
            String[] values = entry.getValue();
            String value = values.length > 0 ? values[0] : null;

            // trouver le paramètre de méthode correspondant (paramètre racine)
            for (java.lang.reflect.Parameter param : methodParams) {
                if (!param.getName().equals(rootName) || !isCustomObject(param.getType())) continue;
                // créer ou récupérer l'instance racine
                Object rootInstance = boundObjects.get(rootName);
                if (rootInstance == null) {
                    try {
                        rootInstance = param.getType().getDeclaredConstructor().newInstance();
                        boundObjects.put(rootName, rootInstance);
                    } catch (Exception e) {
                        // impossible de créer l'instance racine, ignorer
                        break;
                    }
                }

                // Parcourir le chemin jusqu'à l'objet final (gère aussi les listes indexées p.amis[0].nom et p.amis[].nom)
                Object current = rootInstance;
                for (int i = 0; i < path.length - 1; i++) {
                    String token = path[i];
                    PathToken ptoken = parsePathToken(token);
                    if (ptoken.isList) {
                            // Propriété de type liste sur l'objet courant
                        Class<?> elementType = getListElementType(current.getClass(), ptoken.name);
                        if (elementType == null) {
                            current = null;
                            break;
                        }
                        java.util.List<Object> list = getOrCreateListOnParent(current, ptoken.name, elementType);
                        if (list == null) {
                            current = null;
                            break;
                        }
                        if (ptoken.index == null) {
                            // append pour objet imbriqué (ex. p.amis[].nom) : créer un nouvel élément et l'utiliser
                            if (elementType.equals(String.class)) {
                                // impossible d'utiliser [] (append) pour une liste de String imbriquée ; logger et ignorer
                                bindingErrors.put(paramName, "Impossible d'utiliser [] (append) pour une liste de String imbriquée : " + paramName);
                                getServletContext().log("Avertissement de liaison : impossible d'utiliser [] pour une liste de String imbriquée : '" + paramName + "'");
                                current = null;
                                break;
                            } else {
                                java.util.List<Object> listForAppend = getOrCreateListOnParent(current, ptoken.name, elementType);
                                if (listForAppend == null) {
                                    bindingErrors.put(paramName, "Impossible de créer la liste '" + ptoken.name + "' pour '" + paramName + "'");
                                    getServletContext().log("Avertissement de liaison : impossible de créer la liste '" + ptoken.name + "' pour '" + paramName + "'");
                                    current = null;
                                    break;
                                }
                                Object newElem = appendListElement(listForAppend, elementType);
                                if (newElem == null) {
                                    bindingErrors.put(paramName, "Impossible d'instancier un élément de la liste '" + ptoken.name + "' dans '" + paramName + "'");
                                    getServletContext().log("Avertissement de liaison : impossible d'instancier un élément de la liste '" + ptoken.name + "' dans '" + paramName + "'");
                                    current = null;
                                    break;
                                }
                                current = newElem;
                                // On a déjà ajouté un élément et placé current dessus -> ne pas appeler getOrCreateListElement
                                continue;
                            }
                        }
                        // si on a un index explicite, obtenir/créer l'élément à cet index
                        Object elem = getOrCreateListElement(list, ptoken.index, elementType);
                        if (elem == null) {
                            current = null;
                            break;
                        }
                        current = elem;
                    } else {
                        Object child = getOrCreateChildInstance(current, ptoken.name);
                        if (child == null) {
                            current = null; // impossible de continuer
                            break;
                        }
                        current = child;
                    }
                }
                if (current != null) {
                    String lastToken = path[path.length - 1];
                    PathToken lastPtoken = parsePathToken(lastToken);
                    if (lastPtoken.isList) {
                        // La feuille est un élément de liste : ex. p.listEcole[0] ou p.listEcole[]
                        Class<?> elementType = getListElementType(current.getClass(), lastPtoken.name);
                        if (elementType == null) {
                                // Type d'élément inconnu, ignorer
                        } else {
                            java.util.List<Object> list = getOrCreateListOnParent(current, lastPtoken.name, elementType);
                            if (list != null) {
                                if (lastPtoken.index == null) { // append
                                    // pour les listes de type simple (String) ajouter la valeur,
                                    // pour les listes d'objets créer un nouvel élément par valeur et l'ajouter
                                    if (elementType.equals(String.class)) {
                                        // Ajouter toutes les valeurs POSTées pour ce param (p.listEcole[] -> String[])
                                        for (String v : values) {
                                            if (v != null && !v.trim().isEmpty()) {
                                                list.add(v);
                                            }
                                        }
                                    } else {
                                        // Ajout d'objets pour chaque valeur envoyée (même si la valeur en elle-même est ignorée)
                                        for (String v : values) {
                                            Object newElem = appendListElement(list, elementType);
                                            if (newElem == null) {
                                                bindingErrors.put(paramName, "Impossible d'ajouter un nouvel élément de type '" + elementType.getSimpleName() + "' à la liste '" + lastPtoken.name + "'");
                                                getServletContext().log("Avertissement de liaison : impossible d'ajouter un nouvel élément de type '" + elementType.getSimpleName() + "' à la liste '" + lastPtoken.name + "' pour '" + paramName + "'");
                                            } else {
                                                // Si une valeur textuelle n'est pas compatible avec l'objet, on la note
                                                if (v != null && !v.trim().isEmpty()) {
                                                    bindingErrors.put(paramName, "Valeur ignorée lors de l'ajout d'un objet sur la liste '" + lastPtoken.name + "' : " + v);
                                                    getServletContext().log("Avertissement de liaison : valeur ignorée pour l'ajout d'un objet sur '" + lastPtoken.name + "' : '" + v + "'");
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (elementType.equals(String.class)) {
                                        ensureListSize(list, lastPtoken.index + 1);
                                        list.set(lastPtoken.index, value);
                                    } else {
                                        // L'élément est un objet et on a un index explicite : créer/obtenir l'élément
                                        Object elem = getOrCreateListElement(list, lastPtoken.index, elementType);
                                        if (elem == null) {
                                            bindingErrors.put(paramName, "Impossible de créer/lire l'élément indexé '" + lastPtoken.index + "' de la liste '" + lastPtoken.name + "'");
                                            getServletContext().log("Avertissement de liaison : impossible de créer/lire l'élément indexé '" + lastPtoken.index + "' de la liste '" + lastPtoken.name + "' pour '" + paramName + "' ");
                                        } else {
                                            // on ne sait pas comment convertir 'value' en objet complexe -> le signaler
                                            if (value != null && !value.trim().isEmpty()) {
                                                bindingErrors.put(paramName, "Valeur ignorée pour l'élément objet indexé '" + lastPtoken.index + "' de la liste '" + lastPtoken.name + "' : " + value);
                                                getServletContext().log("Avertissement de liaison : valeur ignorée pour l'élément objet indexé '" + lastPtoken.index + "' de la liste '" + lastPtoken.name + "' : '" + value + "'");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        setProperty(current, lastPtoken.name, value);
                    }
                }
                break; // paramètre racine trouvé, arrêter
            }
        }
        if (!bindingErrors.isEmpty()) {
            request.setAttribute("bindingErrors", bindingErrors);
        }
        return boundObjects;
    }

    private static class PathToken {
        String name;
        Integer index; // null si pas d'indice (pas une liste) ou null si append
        boolean isList;
        PathToken(String name, Integer index, boolean isList) {
            this.name = name; this.index = index; this.isList = isList;
        }
    }

    private PathToken parsePathToken(String token) {
        if (token == null) return new PathToken("", null, false);
        int b = token.indexOf('[');
        if (b == -1) {
            return new PathToken(token, null, false);
        }
        int e = token.indexOf(']', b);
        if (e == -1) return new PathToken(token, null, false);
        String name = token.substring(0, b);
        String inside = token.substring(b+1, e);
        if (inside.length() == 0) {
            return new PathToken(name, null, true); // append
        }
        try {
            int idx = Integer.parseInt(inside);
            return new PathToken(name, idx, true);
        } catch (NumberFormatException ex) {
            return new PathToken(name, null, true);
        }
    }

    private Class<?> getListElementType(Class<?> clazz, String listFieldName) {
        // Try setter generic param
            // Essayer le paramètre générique du setter
        String setterName = "set" + listFieldName.substring(0,1).toUpperCase() + listFieldName.substring(1);
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                Type t = m.getGenericParameterTypes()[0];
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length == 1 && args[0] instanceof Class) {
                        return (Class<?>) args[0];
                    }
                }
            }
        }
        // Try field
            // Essayer le champ
        try {
            java.lang.reflect.Field f = clazz.getDeclaredField(listFieldName);
            Type t = f.getGenericType();
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                Type[] args = pt.getActualTypeArguments();
                if (args.length == 1 && args[0] instanceof Class) {
                    return (Class<?>) args[0];
                }
            }
        } catch (Exception e) {
            // ignore
                // ignorer
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    private java.util.List<Object> getOrCreateListOnParent(Object parent, String listName, Class<?> elementType) {
        try {
            Class<?> clazz = parent.getClass();
            String getterName = "get" + listName.substring(0,1).toUpperCase() + listName.substring(1);
            java.util.List<Object> list = null;
            try {
                Method getter = clazz.getMethod(getterName);
                Object o = getter.invoke(parent);
                if (o instanceof java.util.List) list = (java.util.List<Object>) o;
            } catch (Exception e) {
                // ignore
                    // ignorer
            }

            if (list == null) {
                list = new java.util.ArrayList<Object>();
                String setterName = "set" + listName.substring(0,1).toUpperCase() + listName.substring(1);
                Method setter = null;
                for (Method m : clazz.getMethods()) {
                    if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                        setter = m; break;
                    }
                }
                if (setter != null) {
                    setter.invoke(parent, list);
                    return list;
                }
                try {
                    java.lang.reflect.Field f = clazz.getDeclaredField(listName);
                    f.setAccessible(true);
                    f.set(parent, list);
                    return list;
                } catch (Exception e) {
                    // ignorer
                }
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    /** Ajouter un nouvel élément de `elementType` à la `list` et le retourner */
    private Object appendListElement(java.util.List<Object> list, Class<?> elementType) {
        try {
            Object newElem = elementType.getDeclaredConstructor().newInstance();
            list.add(newElem);
            return newElem;
        } catch (Exception e) {
            return null;
        }
    }

    private Object getOrCreateListElement(java.util.List<Object> list, int index, Class<?> elementType) {
        ensureListSize(list, index + 1);
        Object elem = list.get(index);
        if (elem == null) {
            try {
                elem = elementType.getDeclaredConstructor().newInstance();
                list.set(index, elem);
            } catch (Exception e) {
                return null;
            }
        }
        return elem;
    }

    private void ensureListSize(java.util.List<Object> list, int size) {
        while (list.size() < size) list.add(null);
    }

    private Object getOrCreateChildInstance(Object parent, String propertyName) {
        try {
            Class<?> clazz = parent.getClass();
            // essayer le getter
            String getterName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            Method getter = null;
            try {
                getter = clazz.getMethod(getterName);
            } catch (NoSuchMethodException e) {
                // ignorer
            }
            if (getter != null) {
                Object value = getter.invoke(parent);
                if (value != null) return value;
            }

            // Si le getter n'existe pas ou la valeur est null, obtenir le type via setter ou champ
            String setterName = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            Method setter = null;
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                    setter = m;
                    break;
                }
            }
            Class<?> childType = null;
            if (setter != null) childType = setter.getParameterTypes()[0];
            java.lang.reflect.Field field = null;
            if (childType == null) {
                try {
                    field = clazz.getDeclaredField(propertyName);
                    childType = field.getType();
                } catch (NoSuchFieldException nsf) {
                    // introuvable
                }
            }
            if (childType == null) return null;

            // instancier l'enfant
            Object childInstance = null;
            try {
                childInstance = childType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return null; // impossible d'instancier
            }

            // affecter au parent
            if (setter != null) {
                setter.invoke(parent, childInstance);
                return childInstance;
            }
            if (field != null) {
                field.setAccessible(true);
                field.set(parent, childInstance);
                return childInstance;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCustomObject(Class<?> type) {
        return !type.isPrimitive() && !type.equals(String.class) && !type.equals(Map.class) && !type.getName().startsWith("java.");
    }

    private void setProperty(Object instance, String propertyName, String value) {
        try {
            Class<?> clazz = instance.getClass();
            java.lang.reflect.Field field = null;
            try {
                field = clazz.getDeclaredField(propertyName);
            } catch (NoSuchFieldException nsf) {
                // C'est OK si le champ n'est pas trouvé; on peut toujours essayer le setter
            }

            // Essayer le setter (même nom et 1 paramètre)
            String setterName = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            Method setter = null;
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                    setter = m;
                    break;
                }
            }
            if (setter != null) {
                Class<?> paramType = setter.getParameterTypes()[0];
                Object converted = value == null ? null : convertValue(value, paramType);
                setter.invoke(instance, converted);
                return;
            }

            // Essayer le champ
            if (field != null) {
                field.setAccessible(true);
                if (value == null) {
                    field.set(instance, null);
                } else {
                    field.set(instance, convertValue(value, field.getType()));
                }
            }
        } catch (Exception e) {
            // Ignorer si pas de setter ou champ ou si la conversion échoue
        }
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return 0;
        } else if (type == double.class || type == Double.class) {
            return 0.0;
        } else if (type == boolean.class || type == Boolean.class) {
            return false;
        } else if (type == long.class || type == Long.class) {
            return 0L;
        } else if (type == float.class || type == Float.class) {
            return 0.0f;
        } else if (type == char.class || type == Character.class) {
            return '\0';
        } else if (type == byte.class || type == Byte.class) {
            return (byte) 0;
        } else if (type == short.class || type == Short.class) {
            return (short) 0;
        }
        return null;
    }

    private boolean isMapStringObject(java.lang.reflect.Parameter parameter) {
        Type type = parameter.getParameterizedType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getRawType().equals(Map.class)) {
                Type[] args = pt.getActualTypeArguments();
                return args.length == 2 && args[0].equals(String.class) && args[1].equals(Object.class);
            }
        }
        return false;
    }
}