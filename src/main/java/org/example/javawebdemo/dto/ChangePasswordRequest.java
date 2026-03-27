package org.example.javawebdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank(message = "{auth.password.required}")
    @Size(min = 8, max = 64, message = "{auth.password.length}")
    private String oldPassword;

    @NotBlank(message = "{auth.password.required}")
    @Size(min = 8, max = 64, message = "{auth.password.length}")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$", message = "{auth.password.complexity}")
    private String newPassword;

    @NotBlank(message = "{auth.password.required}")
    @Size(min = 8, max = 64, message = "{auth.password.length}")
    private String confirmPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
