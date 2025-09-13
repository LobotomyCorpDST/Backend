package com.devsop.project.apartmentinvoice.repository;

import com.devsop.project.apartmentinvoice.entity.Maintenance;
import com.devsop.project.apartmentinvoice.entity.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class MaintenanceRepository_IntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    private Room testRoom;

    @BeforeEach
    void setUp() {
        testRoom = new Room();
        testRoom.setNumber(101);
        testRoom.setStatus("OCCUPIED");
        entityManager.persist(testRoom);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find completed maintenance within a date range")
    void whenFindByStatusAndDateRange_thenReturnMaintenanceList() {
        LocalDate today = LocalDate.now();

        Maintenance maint1 = Maintenance.builder()
                .room(testRoom).description("Fix AC").status(Maintenance.Status.COMPLETED)
                .scheduledDate(today.minusDays(10)).completedDate(today.minusDays(5))
                .build();
        entityManager.persist(maint1);

        Maintenance maint2 = Maintenance.builder()
                .room(testRoom).description("Paint wall").status(Maintenance.Status.COMPLETED)
                .scheduledDate(today.minusDays(40)).completedDate(today.minusDays(35))
                .build();
        entityManager.persist(maint2);

        Maintenance maint3 = Maintenance.builder()
                .room(testRoom).description("Leaky Faucet").status(Maintenance.Status.PLANNED)
                .scheduledDate(today.minusDays(2)).build();
        entityManager.persist(maint3);
        entityManager.flush();

        List<Maintenance> results = maintenanceRepository.findByRoom_IdAndStatusAndCompletedDateBetween(
                testRoom.getId(),
                Maintenance.Status.COMPLETED,
                today.minusDays(30),
                today
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDescription()).isEqualTo("Fix AC");
    }
}
