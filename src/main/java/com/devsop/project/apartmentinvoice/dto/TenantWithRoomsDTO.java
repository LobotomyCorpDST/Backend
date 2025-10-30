package com.devsop.project.apartmentinvoice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for returning tenant information with their active lease room numbers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantWithRoomsDTO {
    private Long id;
    private String name;
    private String phone;
    private String lineId;
    private List<Integer> roomNumbers; // All active lease room numbers
}
