package com.swiggy.fooddelivery.security;

import com.swiggy.fooddelivery.dto.LoginRequestDto;
import com.swiggy.fooddelivery.dto.LoginResponseDto;
import com.swiggy.fooddelivery.dto.SignUpRequestDto;
import com.swiggy.fooddelivery.dto.SignupResponseDto;
import com.swiggy.fooddelivery.entity.User;
import com.swiggy.fooddelivery.entity.type.AuthProviderType;
import com.swiggy.fooddelivery.entity.type.RoleType;
import com.swiggy.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getUsername(),
                        loginRequestDto.getPassword()
                )
        );
        User user = (User) authentication.getPrincipal();
        String token = authUtil.generateAccessToken(user);
        return new LoginResponseDto(token, user.getId());
    }

    public SignupResponseDto signup(SignUpRequestDto signupRequestDto) {
        User existing = userRepository.findByUsername(signupRequestDto.getUsername()).orElse(null);
        if (existing != null) {
            throw new IllegalArgumentException("User already exists with username: " + signupRequestDto.getUsername());
        }
        Set<RoleType> assignedRoles = (signupRequestDto.getRoles() == null || signupRequestDto.getRoles().isEmpty())
                ? Set.of(RoleType.CUSTOMER)
                : signupRequestDto.getRoles();
        User user = User.builder()
                .username(signupRequestDto.getUsername())
                .password(passwordEncoder.encode(signupRequestDto.getPassword()))
                .providerType(AuthProviderType.EMAIL)
                .roles(assignedRoles)
                .build();
        user = userRepository.save(user);
        return new SignupResponseDto(user.getId(), user.getUsername());
    }
}
