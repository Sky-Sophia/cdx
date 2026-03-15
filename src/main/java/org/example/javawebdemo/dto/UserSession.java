package org.example.javawebdemo.dto;

import org.example.javawebdemo.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSession {
    private Long id;
    private String username;
    private Role role;
}
