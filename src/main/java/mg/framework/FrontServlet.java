package mg.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


@WebServlet(name = "FrontServlet", urlPatterns = {"/"}, loadOnStartup = 1)
public class FrontServlet extends HttpServlet {
    
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
        
        showFrameworkPage(request, response, resourcePath);
    }
    
    private void showFrameworkPage(HttpServletRequest request, HttpServletResponse response, 
                                 String requestedPath) 
            throws IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<!DOCTYPE html>");
        out.println("<html lang='fr'>");
        out.println("<head>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("    <title>Framework Java - Page non trouvée</title>");
        out.println("    <style>");
        out.println("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 40px; background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); color: #212529; min-height: 100vh; }");
        out.println("        .container { max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); border: 1px solid #e9ecef; }");
        out.println("        h1 { color: #212529; text-align: center; margin-bottom: 30px; padding-bottom: 15px; border-bottom: 2px solid #333; font-weight: 300; font-size: 2.5em; }");
        out.println("        .message { background: #f8f9fa; padding: 25px; border-radius: 12px; border-left: 4px solid #333; margin: 25px 0; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }");
        out.println("        .path { font-family: 'Courier New', monospace; background: #e9ecef; padding: 12px 16px; border-radius: 8px; display: inline-block; margin: 15px 0; font-weight: bold; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }");
        out.println("        .footer { margin-top: 30px; text-align: center; color: #6c757d; font-size: 14px; padding-top: 20px; border-top: 1px solid #dee2e6; }");
        out.println("        p { line-height: 1.6; margin-bottom: 15px; color: #495057; }");
        out.println("        h3 { color: #333; margin-bottom: 15px; font-weight: 500; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='container'>");
        out.println("        <h1>FRAMEWORK JAVA</h1>");
        
        out.println("        <div class='message'>");
        out.println("            <h3>Ressource non trouvée</h3>");
        out.println("            <p>Voici l'URL demandée :</p>");
        out.println("            <div class='path'><strong>" + requestedPath + "</strong></div>");
        out.println("        </div>");
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
    }
}