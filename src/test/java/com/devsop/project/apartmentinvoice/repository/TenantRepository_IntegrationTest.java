package com.devsop.project.apartmentinvoice.repository;

import com.devsop.project.apartmentinvoice.entity.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TenantRepository_IntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @DisplayName("Should save a tenant and then find them by ID")
    void whenSaveTenant_thenFindById() {
        Tenant newTenant = new Tenant();
        newTenant.setName("John Doe");
        newTenant.setPhone("123-456-7890");
        entityManager.persist(newTenant);
        entityManager.flush();

        Optional<Tenant> foundTenantOpt = tenantRepository.findById(newTenant.getId());

        assertThat(foundTenantOpt).isPresent();
        assertThat(foundTenantOpt.get().getName()).isEqualTo("John Doe");
    }
}
