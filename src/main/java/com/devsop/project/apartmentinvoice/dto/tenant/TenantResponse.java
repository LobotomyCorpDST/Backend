package com.devsop.project.apartmentinvoice.dto.tenant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantResponse {
  Long id;
  String name;
  String phone;
  String lineId;
}
