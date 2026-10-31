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
        return ResponseEntity.ok("Order placed");
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
