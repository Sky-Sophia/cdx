package org.example.javawebdemo.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String passwordSalt;
    private Role role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
