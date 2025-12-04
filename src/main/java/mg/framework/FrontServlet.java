package mg.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


@WebServlet(name = "FrontServlet", urlPatterns = {"/"}, loadOnStartup = 1)
public class FrontServlet extends HttpServlet {
    private mg.framework.registry.ControllerRegistry registry;

    @Override
    public void init() throws ServletException {
        super.init();
        Object attr = getServletContext().getAttribute(mg.framework.init.FrameworkInitializer.REGISTRY_ATTR);
        if (attr instanceof mg.framework.registry.ControllerRegistry) {
            this.registry = (mg.framework.registry.ControllerRegistry) attr;
            getServletContext().log("FrontServlet: registry loaded with " + this.registry.getExactRoutesSnapshot().size() + " exact routes");
        } else {
            getServletContext().log("FrontServlet: no ControllerRegistry found in ServletContext");
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

        try {
            java.net.URL resource = getServletContext().getResource(resourcePath);
            if (resource != null) {
                RequestDispatcher defaultServlet = getServletContext().getNamedDispatcher("default");
                if (defaultServlet != null) {
                    defaultServlet.forward(request, response);
                    return;
                }
            }
        } catch (Exception e) {
            throw new ServletException("Erreur lors de la vérification de la ressource: " + resourcePath, e);
        }

        if (registry != null) {
            java.util.List<mg.framework.registry.HandlerMethod> handlers = registry.findExact(resourcePath);
            if (handlers != null && !handlers.isEmpty()) {
                for (mg.framework.registry.HandlerMethod h : handlers) {
                    response.getWriter().println(h.getControllerClass().getName() + "#" + h.getMethod().getName());
                }
                return;
            }
        }

        response.getWriter().println("Ressource non trouvée: " + resourcePath);

    }

}