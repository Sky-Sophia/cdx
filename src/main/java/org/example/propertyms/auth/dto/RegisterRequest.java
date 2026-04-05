package org.example.propertyms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {
    @NotBlank(message = "{auth.username.required}")
    @Size(min = 3, max = 20, message = "{auth.username.length}")
    @Pattern(regexp = "^\\S+$", message = "{auth.username.space}")
    private String username;

    @NotBlank(message = "{auth.password.required}")
    @Size(min = 8, max = 64, message = "{auth.password.length}")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$", message = "{auth.password.complexity}")
    private String password;

    @NotBlank(message = "{auth.password.required}")
    @Size(min = 8, max = 64, message = "{auth.password.length}")
    private String confirmPassword;
}

