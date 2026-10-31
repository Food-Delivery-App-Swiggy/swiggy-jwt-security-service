package com.swiggy.fooddelivery.dto;

import com.swiggy.fooddelivery.entity.type.RoleType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.HashSet;
import java.util.Set;

@Data
public class SignUpRequestDto {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private Set<RoleType> roles = new HashSet<>();
}
