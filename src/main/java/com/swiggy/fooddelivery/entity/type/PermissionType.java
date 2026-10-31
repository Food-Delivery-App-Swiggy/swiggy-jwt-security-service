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
