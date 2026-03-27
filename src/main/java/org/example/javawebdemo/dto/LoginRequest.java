package org.example.javawebdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @NotBlank(message = "{auth.username.required}")
    @Size(min = 3, max = 20, message = "{auth.username.length}")
    @Pattern(regexp = "^\\S+$", message = "{auth.username.space}")
    private String username;

    @NotBlank(message = "{auth.password.required}")
    @Size(min = 8, max = 64, message = "{auth.password.length}")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
