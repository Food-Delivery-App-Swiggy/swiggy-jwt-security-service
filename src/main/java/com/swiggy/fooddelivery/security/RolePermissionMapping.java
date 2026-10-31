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
