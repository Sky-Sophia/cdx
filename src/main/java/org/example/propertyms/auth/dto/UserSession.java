package org.example.propertyms.auth.dto;

import org.example.propertyms.user.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSession {
    private Long id;
    private String username;
    private Role role;
}

