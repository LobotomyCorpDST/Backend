package com.devsop.project.apartmentinvoice.dto;

import com.devsop.project.apartmentinvoice.entity.Room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private Long id;
    private Integer roomNumber;

    public static RoomResponse from(Room r) {
        return RoomResponse.builder()
                .id(r.getId())
                .roomNumber(r.getNumber()) // ถ้า field ใน Room ไม่ใช่ "number" ให้บอกผมจะปรับให้
                .build();
    }
}
