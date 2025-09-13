package com.devsop.project.apartmentinvoice.repository;

import com.devsop.project.apartmentinvoice.entity.Lease;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class LeaseRepository_IntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LeaseRepository leaseRepository;

    private Room testRoom1;
    private Tenant testTenant1;

    @BeforeEach
    void setup() {
        testTenant1 = new Tenant();
        testTenant1.setName("Alice");
        entityManager.persist(testTenant1);

        testRoom1 = new Room();
        testRoom1.setNumber(202);
        testRoom1.setStatus("AVAILABLE");
        entityManager.persist(testRoom1);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find an active lease for a room on a specific date")
    void whenFindActiveLeaseByRoomOnDate_thenReturnLease() {
        LocalDate today = LocalDate.now();
        Lease activeLease = Lease.builder()
                .room(testRoom1)
                .tenant(testTenant1)
                .startDate(today.minusMonths(6))
                .endDate(today.plusMonths(6))
                .monthlyRent(new BigDecimal("5000"))
                .status(Lease.Status.ACTIVE)
                .build();
        entityManager.persist(activeLease);

        Optional<Lease> foundLease = leaseRepository.findActiveLeaseByRoomOnDate(testRoom1.getId(), today);

        assertThat(foundLease).isPresent();
        assertThat(foundLease.get().getId()).isEqualTo(activeLease.getId());
    }
}
