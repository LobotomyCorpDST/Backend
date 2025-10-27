package com.devsop.project.apartmentinvoice.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BulkPrintRequest {

    @NotEmpty(message = "IDs list cannot be empty")
    @Size(min = 1, max = 100, message = "Bulk print supports 1 to 100 items at a time")
    private List<Long> ids;
}
