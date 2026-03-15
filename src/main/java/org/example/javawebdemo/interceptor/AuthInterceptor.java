package org.example.javawebdemo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.javawebdemo.dto.UserSession;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.util.SessionKeys;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isPublic(path, method)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        UserSession user = session == null ? null : (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (user == null) {
            response.sendRedirect("/login");
            return false;
        }

        if (path.startsWith("/admin")) {
            if (user.getRole() != Role.ADMIN) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }

        if (path.startsWith("/staff")) {
            if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }

        if ("/orders".equals(path)) {
            if (user.getRole() == Role.ADMIN) {
                response.sendRedirect("/admin/orders");
                return false;
            }
            if (user.getRole() == Role.STAFF) {
                response.sendRedirect("/staff/orders");
                return false;
            }
        }

        return true;
    }

    private boolean isPublic(String path, String method) {
        if (path.equals("/") || path.equals("/login") || path.equals("/register") || path.equals("/logout")) {
            return true;
        }
        if (path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/images") || path.startsWith("/uploads")) {
            return true;
        }
        if (path.equals("/error")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/movies") || path.startsWith("/shows")) {
                return true;
            }
        }
        return false;
    }
}
