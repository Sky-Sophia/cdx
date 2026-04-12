package org.example.propertyms.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.user.model.Role;
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

        Role role = user.getRole();
        if (path.startsWith("/actuator") && (role == null || !role.canManageUsers())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        if (path.startsWith("/admin/users") && (role == null || !role.canManageUsers())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        if (path.startsWith("/admin") && (role == null || !role.canAccessAdminConsole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    private boolean isPublic(String path) {
        return "/login".equals(path)
                || "/register".equals(path)
                || "/logout".equals(path)
                || "/forgot-password".equals(path)
                || "/error".equals(path)
                || path.startsWith("/css")
                || path.startsWith("/js")
                || path.startsWith("/images")
                || path.startsWith("/icons")
                || path.startsWith("/favicon")
                || path.startsWith("/webjars");
    }
}
