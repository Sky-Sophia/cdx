package org.example.javawebdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "{auth.username.required}")
    @Size(min = 3, max = 20, message = "{auth.username.length}")
    @Pattern(regexp = "^\\S+$", message = "{auth.username.space}")
    private String username;

    @NotBlank(message = "{auth.password.required}")
    @Size(min = 6, max = 32, message = "{auth.password.length}")
    private String password;

    @NotBlank(message = "{auth.password.required}")
    @Size(min = 6, max = 32, message = "{auth.password.length}")
    private String confirmPassword;

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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
