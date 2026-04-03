package org.example.javawebdemo.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.example.javawebdemo.dto.UserSession;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.util.SessionKeys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class AuthInterceptorTest {

    private final AuthInterceptor authInterceptor = new AuthInterceptor();

    @Test
    void preHandle_shouldAllowPublicPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
    }

    @Test
    void preHandle_shouldRedirectWhenUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/management");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals("/login", response.getRedirectedUrl());
    }

    @Test
    void preHandle_shouldBlockNonAdminFromUserManagement() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(1L, "staff", Role.STAFF));
        request.setSession(session);

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandle_shouldAllowAdminPathForAdmin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.CURRENT_USER, new UserSession(2L, "admin", Role.ADMIN));
        request.setSession(session);

        boolean allowed = authInterceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
    }
}
