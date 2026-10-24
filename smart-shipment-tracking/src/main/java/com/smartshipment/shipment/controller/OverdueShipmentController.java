package com.smartshipment.shipment.controller;

import java.util.List;
import com.smartshipment.shipment.dto.OverdueShipmentResponse;
import com.smartshipment.shipment.service.OverdueShipmentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OverdueShipmentController {
    private final OverdueShipmentService service;

    public OverdueShipmentController(OverdueShipmentService service) {
        this.service = service;
    }

    @GetMapping("/api/shipments/overdue")
    public List<OverdueShipmentResponse> findOverdue(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.findOverdue(page, size);
    }
}
