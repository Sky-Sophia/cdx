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

        if (isPublic(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        UserSession user = session == null ? null : (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (user == null) {
            response.sendRedirect("/login");
            return false;
        }

        if (path.startsWith("/admin") && user.getRole() == Role.USER) {
            response.sendRedirect("/logout");
            return false;
        }

        return true;
    }

    private boolean isPublic(String path) {
        return "/login".equals(path)
                || "/register".equals(path)
                || "/logout".equals(path)
                || "/error".equals(path)
                || path.startsWith("/css")
                || path.startsWith("/images")
                || path.startsWith("/favicon")
                || path.startsWith("/webjars")
                || path.startsWith("/actuator");
    }
}
