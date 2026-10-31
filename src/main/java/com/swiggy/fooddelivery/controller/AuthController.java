package com.swiggy.fooddelivery.controller;

import com.swiggy.fooddelivery.dto.LoginRequestDto;
import com.swiggy.fooddelivery.dto.LoginResponseDto;
import com.swiggy.fooddelivery.dto.SignUpRequestDto;
import com.swiggy.fooddelivery.dto.SignupResponseDto;
import com.swiggy.fooddelivery.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody SignUpRequestDto signupRequestDto) {
        return ResponseEntity.ok(authService.signup(signupRequestDto));
    }
}
