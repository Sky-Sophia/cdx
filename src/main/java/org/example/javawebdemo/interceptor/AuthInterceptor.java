package org.example.javawebdemo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import org.example.javawebdemo.dto.UserSession;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.util.SessionKeys;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {
        String path = request.getRequestURI();

        if (isPublic(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        UserSession user = session == null ? null : (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (user == null) {
            if (path.startsWith("/actuator")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            response.sendRedirect("/login");
            return false;
        }

        if (path.startsWith("/actuator") && user.getRole() != Role.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        if (path.startsWith("/admin/users") && user.getRole() != Role.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        if (path.startsWith("/admin") && user.getRole() == Role.USER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
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
                || path.startsWith("/webjars");
    }
}
