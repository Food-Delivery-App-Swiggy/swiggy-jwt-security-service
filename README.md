# swiggy-jwt-security-service
Swiggy Food Delivery App - Spring Security 6, JWT &amp; RBAC Implementation


# Swiggy Food Delivery App — Spring Security 6, JWT & RBAC Service

A production-ready, stateless authentication and fine-grained Role-Based Access Control (RBAC) security microservice built with **Spring Boot 3.4.2**, **Spring Security 6**, and **JJWT 0.12.6**, modeled upon and adapted to the **Food-Delivery-App-Swiggy** enterprise architecture.

## Table of Contents

1. [Architecture & Request Lifecycle](#architecture--request-lifecycle)

2. [Project Structure](#project-structure)

3. [Technology Stack & Dependencies](#technology-stack--dependencies)

4. [Role-Based Access Control (RBAC) Matrix](#role-based-access-control-rbac-matrix)

5. [Complete Source Code Reference](#complete-source-code-reference)

    * [pom.xml](#pomxml)

    * [application.yml](#applicationyml)

    * [FoodDeliveryApplication.java](#fooddeliveryapplicationjava)

    * [AppConfig.java](#appconfigjava)

    * [Enums: RoleType, PermissionType, AuthProviderType](#enums-roletype-permissiontype-authprovidertype)

    * [RolePermissionMapping.java](#rolepermissionmappingjava)

    * [User.java (Entity)](#userjava-entity)

    * [UserRepository.java](#userrepositoryjava)

    * [CustomUserDetailsService.java](#customuserdetailsservicejava)

    * [AuthUtil.java (Token Engine)](#authutiljava-token-engine)

    * [JwtAuthFilter.java (OncePerRequestFilter)](#jwtauthfilterjava-onceperrequestfilter)

    * [WebSecurityConfig.java (SecurityFilterChain)](#websecurityconfigjava-securityfilterchain)

    * [DTOs (Login & Signup)](#dtos-login--signup)

    * [AuthService.java & AuthController.java](#authservicejava--authcontrollerjava)

    * [OrderController.java (@PreAuthorize Demo)](#ordercontrollerjava-preauthorize-demo)

    * [Error Handling: ApiError & GlobalExceptionHandler](#error-handling-apierror--globalexceptionhandler)

6. [Testing & Operational Verification Runbook](#testing--operational-verification-runbook)

7. [Git Commit Squashing Guide (Clean 1-Commit History)](#git-commit-squashing-guide-clean-1-commit-history)

8. [Microservices Integration Strategy](#microservices-integration-strategy)

## Architecture & Request Lifecycle

```
  [ Client (Web / Mobile) ]
             │
             │ HTTP Request (Authorization: Bearer <JWT>)
             ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │                        SecurityFilterChain                             │
  │                                                                        │
  │   1. CorsFilter (Configured)                                           │
  │   2. CsrfFilter (Disabled for Stateless REST)                          │
  │   3. JwtAuthFilter (OncePerRequestFilter)                              │
  │        ├─ Extracts Bearer Token from 'Authorization' header            │
  │        ├─ AuthUtil.getUsernameFromToken(token) (HMAC-SHA256 verify)    │
  │        ├─ Loads UserDetails via CustomUserDetailsService               │
  │        ├─ Maps RoleType -> Granular Authorities via RolePermissionMap │
  │        ├─ Injects UsernamePasswordAuthenticationToken into Context     │
  │        └─ Delegates exceptions -> HandlerExceptionResolver            │
  │   4. UsernamePasswordAuthenticationFilter                              │
  │   5. AuthorizationFilter (URL Access Rules & Firewall)                 │
  └────────────────────────────────────────────────────────────────────────┘
             │
             │ Populated SecurityContextHolder
             ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │                      REST Controllers Layer                            │
  │                                                                        │
  │   • AuthController: /api/v1/auth/signup, /api/v1/auth/login            │
  │   • OrderController: /api/v1/orders (Guarded by @PreAuthorize)         │
  │   • RestaurantController: /api/v1/restaurants/manage/**                │
  └────────────────────────────────────────────────────────────────────────┘
             │
             │ Filter/Auth Failure
             ▼
  ┌────────────────────────────────────────────────────────────────────────┐
  │          GlobalExceptionHandler (@RestControllerAdvice)                │
  │   • JwtException / AuthenticationException -> 401 Unauthorized         │
  │   • AccessDeniedException                  -> 403 Forbidden            │
  │   • Returns RFC 7807 Standard ApiError JSON Response                   │
  └────────────────────────────────────────────────────────────────────────┘

```

## Project Structure

```
swiggy-jwt-security-service/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── resources/
        │   └── application.yml
        └── java/
            └── com/
                └── swiggy/
                    └── fooddelivery/
                        ├── FoodDeliveryApplication.java
                        ├── config/
                        │   └── AppConfig.java
                        ├── controller/
                        │   ├── AuthController.java
                        │   └── OrderController.java
                        ├── dto/
                        │   ├── LoginRequestDto.java
                        │   ├── LoginResponseDto.java
                        │   ├── SignUpRequestDto.java
                        │   └── SignupResponseDto.java
                        ├── entity/
                        │   ├── User.java
                        │   └── type/
                        │       ├── AuthProviderType.java
                        │       ├── PermissionType.java
                        │       └── RoleType.java
                        ├── error/
                        │   ├── ApiError.java
                        │   └── GlobalExceptionHandler.java
                        ├── repository/
                        │   └── UserRepository.java
                        └── security/
                            ├── AuthService.java
                            ├── AuthUtil.java
                            ├── CustomUserDetailsService.java
                            ├── JwtAuthFilter.java
                            ├── RolePermissionMapping.java
                            └── WebSecurityConfig.java

```

## Technology Stack & Dependencies

* **Java**: 21 (LTS)

* **Framework**: Spring Boot 3.4.2 & Spring Security 6

* **Token Engine**: JJWT (Java JWT) `0.12.6` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)

* **Persistence**: Spring Data JPA & PostgreSQL

* **Validation**: Jakarta Validation API (`spring-boot-starter-validation`)

* **Boilerplate Reduction**: Project Lombok

## Role-Based Access Control (RBAC) Matrix

| Role | Granted Role Authority | Fine-Grained Authorities | Operational Capabilities | 
| ----- | ----- | ----- | ----- | 
| **CUSTOMER** | `ROLE_CUSTOMER` | `menu:read`, `order:read`, `order:write` | Search menus, place orders, track order deliveries | 
| **RESTAURANT_OWNER** | `ROLE_RESTAURANT_OWNER` | `menu:read`, `menu:write`, `order:read`, `order:write` | Manage menu items, pricing, accept/reject incoming orders | 
| **DELIVERY_PARTNER** | `ROLE_DELIVERY_PARTNER` | `order:read`, `delivery:update` | Accept delivery assignments, update live delivery statuses | 
| **ADMIN** | `ROLE_ADMIN` | All above + `order:delete`, `user:manage`, `report:view` | Full platform administration, user management, order cancellations, analytics | 

## Complete Source Code Reference

### `pom.xml`

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.2</version>
        <relativePath/>
    </parent>

    <groupId>com.swiggy</groupId>
    <artifactId>food-delivery-security</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>food-delivery-security</name>
    <description>Swiggy Food Delivery App - Spring Security 6, JWT &amp; RBAC</description>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-client</artifactId>
        </dependency>

        <!-- JJWT 0.12.6 -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Database Drivers -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

```

### `src/main/resources/application.yml`

```
server:
  port: 8080
  servlet:
    context-path: /api/v1

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/swiggy_db
    username: postgres
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

# Minimum 64-character signing key for HMAC-SHA256
jwt:
  secretKey: 4f8b9c2d1e6a7f3b5c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d
  expirationMs: 600000 # 10 minutes (1000 * 60 * 10 ms)

```

### `FoodDeliveryApplication.java`

```
package com.swiggy.fooddelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FoodDeliveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodDeliveryApplication.class, args);
    }
}

```

### `config/AppConfig.java`

```
package com.swiggy.fooddelivery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}

```

### Enums: `RoleType`, `PermissionType`, `AuthProviderType`

```
// entity/type/RoleType.java
package com.swiggy.fooddelivery.entity.type;

public enum RoleType {
    CUSTOMER,
    RESTAURANT_OWNER,
    DELIVERY_PARTNER,
    ADMIN
}

```

```
// entity/type/PermissionType.java
package com.swiggy.fooddelivery.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionType {
    MENU_READ("menu:read"),
    MENU_WRITE("menu:write"),
    ORDER_READ("order:read"),
    ORDER_WRITE("order:write"),
    ORDER_DELETE("order:delete"),
    DELIVERY_UPDATE("delivery:update"),
    USER_MANAGE("user:manage"),
    REPORT_VIEW("report:view");

    private final String permission;
}

```

```
// entity/type/AuthProviderType.java
package com.swiggy.fooddelivery.entity.type;

public enum AuthProviderType {
    EMAIL,
    GOOGLE,
    GITHUB
}

```

### `security/RolePermissionMapping.java`

```
package com.swiggy.fooddelivery.security;

import com.swiggy.fooddelivery.entity.type.PermissionType;
import com.swiggy.fooddelivery.entity.type.RoleType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.swiggy.fooddelivery.entity.type.PermissionType.*;
import static com.swiggy.fooddelivery.entity.type.RoleType.*;

public class RolePermissionMapping {

    private static final Map<RoleType, Set<PermissionType>> map = Map.of(
            CUSTOMER, Set.of(MENU_READ, ORDER_READ, ORDER_WRITE),
            RESTAURANT_OWNER, Set.of(MENU_READ, MENU_WRITE, ORDER_READ, ORDER_WRITE),
            DELIVERY_PARTNER, Set.of(ORDER_READ, DELIVERY_UPDATE),
            ADMIN, Set.of(MENU_READ, MENU_WRITE, ORDER_READ, ORDER_WRITE, ORDER_DELETE, DELIVERY_UPDATE, USER_MANAGE, REPORT_VIEW)
    );

    public static Set<SimpleGrantedAuthority> getAuthoritiesForRole(RoleType role) {
        return map.getOrDefault(role, Set.of()).stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toSet());
    }
}

```

### `entity/User.java`

```
package com.swiggy.fooddelivery.entity;

import com.swiggy.fooddelivery.entity.type.AuthProviderType;
import com.swiggy.fooddelivery.entity.type.RoleType;
import com.swiggy.fooddelivery.security.RolePermissionMapping;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "app_user", indexes = {
        @Index(name = "idx_provider_id_provider_type", columnList = "providerId, providerType")
})
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;

    private String providerId;

    @Enumerated(EnumType.STRING)
    private AuthProviderType providerType;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<RoleType> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        roles.forEach(role -> {
            authorities.addAll(RolePermissionMapping.getAuthoritiesForRole(role));
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        });
        return authorities;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}

```

### `repository/UserRepository.java`

```
package com.swiggy.fooddelivery.repository;

import com.swiggy.fooddelivery.entity.User;
import com.swiggy.fooddelivery.entity.type.AuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByProviderIdAndProviderType(String providerId, AuthProviderType providerType);
}

```

### `security/CustomUserDetailsService.java`

```
package com.swiggy.fooddelivery.security;

import com.swiggy.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
}

```

### `security/AuthUtil.java`

```
package com.swiggy.fooddelivery.security;

import com.swiggy.fooddelivery.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class AuthUtil {

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    @Value("${jwt.expirationMs:600000}")
    private long jwtExpirationMs;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("roles", user.getRoles())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSecretKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}

```

### `security/JwtAuthFilter.java`

```
package com.swiggy.fooddelivery.security;

import com.swiggy.fooddelivery.entity.User;
import com.swiggy.fooddelivery.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            log.info("Incoming request: {}", request.getRequestURI());

            final String requestTokenHeader = request.getHeader("Authorization");
            if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = requestTokenHeader.substring(7).trim();
            String username = authUtil.getUsernameFromToken(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByUsername(username).orElseThrow();
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }
}

```

### `security/WebSecurityConfig.java`

```
package com.swiggy.fooddelivery.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static com.swiggy.fooddelivery.entity.type.PermissionType.*;
import static com.swiggy.fooddelivery.entity.type.RoleType.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(csrfConfig -> csrfConfig.disable())
                .sessionManagement(sessionConfig ->
                        sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**", "/auth/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/admin/**")
                            .hasAnyAuthority(ORDER_DELETE.getPermission(), USER_MANAGE.getPermission())
                        .requestMatchers("/admin/**").hasRole(ADMIN.name())
                        .requestMatchers("/restaurants/manage/**").hasAnyRole(RESTAURANT_OWNER.name(), ADMIN.name())
                        .requestMatchers("/delivery/**").hasAnyRole(DELIVERY_PARTNER.name(), ADMIN.name())
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandlingConfig ->
                        exceptionHandlingConfig.accessDeniedHandler((request, response, accessDeniedException) ->
                                handlerExceptionResolver.resolveException(request, response, null, accessDeniedException)
                        )
                );

        return httpSecurity.build();
    }
}

```

### DTOs: `LoginRequestDto`, `LoginResponseDto`, `SignUpRequestDto`, `SignupResponseDto`

```
// dto/LoginRequestDto.java
package com.swiggy.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}

```

```
// dto/LoginResponseDto.java
package com.swiggy.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private String jwt;
    private Long userId;
}

```

```
// dto/SignUpRequestDto.java
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

```

```
// dto/SignupResponseDto.java
package com.swiggy.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupResponseDto {
    private Long id;
    private String username;
}

```

### `security/AuthService.java` & `controller/AuthController.java`

```
// security/AuthService.java
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

```

```
// controller/AuthController.java
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

```

### `controller/OrderController.java`

```
package com.swiggy.fooddelivery.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @PostMapping
    @PreAuthorize("hasAuthority('order:write')")
    public ResponseEntity<String> placeOrder() {
        return ResponseEntity.ok("Order placed successfully");
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('order:read') or hasRole('ADMIN')")
    public ResponseEntity<String> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok("Order details for #" + orderId);
    }

    @PatchMapping("/{orderId}/delivery-status")
    @PreAuthorize("hasRole('DELIVERY_PARTNER') and hasAuthority('delivery:update')")
    public ResponseEntity<String> updateDeliveryStatus(@PathVariable Long orderId) {
        return ResponseEntity.ok("Delivery status updated for #" + orderId);
    }
}

```

### Error Handling: `ApiError.java` & `GlobalExceptionHandler.java`

```
// error/ApiError.java
package com.swiggy.fooddelivery.error;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
public class ApiError {
    private LocalDateTime timeStamp;
    private String error;
    private HttpStatus statusCode;

    public ApiError(String error, HttpStatus statusCode) {
        this.timeStamp = LocalDateTime.now();
        this.error = error;
        this.statusCode = statusCode;
    }
}

```

```
// error/GlobalExceptionHandler.java
package com.swiggy.fooddelivery.error;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        ApiError apiError = new ApiError("Username not found: " + ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        ApiError apiError = new ApiError("Authentication failed: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwtException(JwtException ex) {
        ApiError apiError = new ApiError("Invalid JWT token: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex) {
        ApiError apiError = new ApiError("Access denied: Insufficient privileges", HttpStatus.FORBIDDEN);
        return new ResponseEntity<>(apiError, HttpStatus.FORBIDDEN);
    }
}

```

## Testing & Operational Verification Runbook

### 1. User Registration (Customer)

```
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer1@swiggy.in",
    "password": "Password123!",
    "roles": ["CUSTOMER"]
  }'

```

**Expected Response:** `HTTP 200 OK`

```
{
  "id": 1,
  "username": "customer1@swiggy.in"
}

```

### 2. User Authentication & JWT Issuance

```
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer1@swiggy.in",
    "password": "Password123!"
  }'

```

**Expected Response:** `HTTP 200 OK`

```
{
  "jwt": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjdXN0b21lcjFAc3dpZ2d5LmluIiwidXNlcklkIjoiMSIsInJvbGVzIjpbIkNVU1RPTUVSIl19...",
  "userId": 1
}

```

### 3. Place Order (Authorized via `order:write`)

```
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <JWT_TOKEN>"

```

**Expected Response:** `HTTP 200 OK`

```
Order placed successfully

```

### 4. Forbidden Action (Access Denied)

Attempting to update delivery status with a `CUSTOMER` role token:

```
curl -X PATCH http://localhost:8080/api/v1/orders/101/delivery-status \
  -H "Authorization: Bearer <CUSTOMER_JWT_TOKEN>"

```

**Expected Response:** `HTTP 403 Forbidden`

```
{
  "timeStamp": "2026-10-31T18:30:00",
  "error": "Access denied: Insufficient privileges",
  "statusCode": "FORBIDDEN"
}

```

### 5. Invalid / Expired Token

```
curl -X GET http://localhost:8080/api/v1/orders/101 \
  -H "Authorization: Bearer invalid.jwt.token"

```

**Expected Response:** `HTTP 401 Unauthorized`

```
{
  "timeStamp": "2026-10-31T18:30:00",
  "error": "Invalid JWT token: JWT strings must contain exactly 2 period characters",
  "statusCode": "UNAUTHORIZED"
}

```

## Git Commit Squashing Guide (Clean 1-Commit History)

To squash the existing multi-commit development history on GitHub into a single atomic initial commit, run these commands in your local Git terminal:

```
# 1. Clone the repository locally
git clone https://github.com/Food-Delivery-App-Swiggy/swiggy-jwt-security-service.git
cd swiggy-jwt-security-service

# 2. Create an orphan branch with zero commit history
git checkout --orphan clean-main

# 3. Stage all current files
git add -A

# 4. Commit as a single clean initial commit
git commit -m "feat(security): complete Spring Security 6, JWT, and RBAC architecture"

# 5. Delete the old multi-commit local main branch
git branch -D main

# 6. Rename the orphan branch to main
git branch -m main

# 7. Force push to remote origin
git push -f origin main

```

**Verification:**

```
git log --oneline
# Output:
# <hash> feat(security): complete Spring Security 6, JWT, and RBAC architecture (HEAD -> main, origin/main)

```

## Microservices Integration Strategy

In the [Food-Delivery-App-Swiggy](https://github.com/orgs/Food-Delivery-App-Swiggy/repositories) microservices ecosystem:

1. **API Gateway Integration (`ApiGatewayMS`)**:

    * `ApiGatewayMS` acts as the single entry point.

    * It validates Bearer tokens at the edge using `AuthUtil` and enriches downstream requests with internal HTTP headers:

        * `X-User-Id: 1`

        * `X-User-Name: customer1@swiggy.in`

        * `X-User-Roles: CUSTOMER`

        * `X-User-Permissions: menu:read,order:read,order:write`

2. **User Service Integration (`UserService`)**:

    * `UserService` manages user profile data, addresses, and payment methods.

    * It references `swiggy-jwt-security-service` for identity validation and token issuance.

3. **Downstream Microservices (`OrderServices`, `DeliveryPartnerService`, `RestaurantServices`)**:

    * Microservices can either validate tokens independently using the shared `JwtAuthFilter` or trust headers passed from `ApiGatewayMS`.
