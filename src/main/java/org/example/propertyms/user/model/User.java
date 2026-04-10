package org.example.propertyms.user.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private Role role;
    private String status;
    private Long unitId;
    private String departmentCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
