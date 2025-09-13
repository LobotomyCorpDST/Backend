package com.devsop.project.apartmentinvoice.controller;

import com.devsop.project.apartmentinvoice.entity.Tenant;
import com.devsop.project.apartmentinvoice.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;

@WebMvcTest(TenantController.class)
public class TenantController_UnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantRepository tenantRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/tenants should return a list of tenants")
    void whenGetAllTenants_thenReturnJsonArray() throws Exception {
        Tenant tenant1 = new Tenant(1L, "Alice", "111-222-3333", "alice_line");
        Tenant tenant2 = new Tenant(2L, "Bob", "444-555-6666", "bob_line");
        List<Tenant> tenants = List.of(tenant1, tenant2);

        when(tenantRepository.findAll()).thenReturn(tenants);

        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Alice")))
                .andExpect(jsonPath("$[1].name", is("Bob")));
    }

    @Test
    @DisplayName("POST /api/tenants should create a new tenant")
    void whenCreateTenant_thenReturnSavedTenant() throws Exception {
        Tenant newTenant = new Tenant(null, "Charlie", "777-888-9999", "charlie_line");
        Tenant savedTenant = new Tenant(1L, "Charlie", "777-888-9999", "charlie_line");

        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTenant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Charlie")));
    }

    @Test
    @DisplayName("PUT /api/tenants/{id} should update an existing tenant")
    void whenUpdateTenant_thenReturnUpdatedTenant() throws Exception {
        long tenantId = 1L;
        Tenant updatedInfo = new Tenant(null, "Alicia Updated", "111-222-3334", "alicia_updated");
        Tenant resultTenant = new Tenant(tenantId, "Alicia Updated", "111-222-3334", "alicia_updated");

        when(tenantRepository.save(any(Tenant.class))).thenReturn(resultTenant);

        mockMvc.perform(put("/api/tenants/{id}", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int)tenantId)))
                .andExpect(jsonPath("$.name", is("Alicia Updated")));
    }

    @Test
    @DisplayName("DELETE /api/tenants/{id} should return status 200 OK")
    void whenDeleteTenant_thenReturnOk() throws Exception {
        long tenantId = 1L;

        mockMvc.perform(delete("/api/tenants/{id}", tenantId))
                .andExpect(status().isOk());
    }
}
