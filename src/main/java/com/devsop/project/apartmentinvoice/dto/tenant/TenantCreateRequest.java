package com.devsop.project.apartmentinvoice.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantCreateRequest {
  @NotBlank @Size(max = 150)
  private String name;

  @Size(max = 30)
  private String phone;

  @Size(max = 50)
  private String lineId;
}
