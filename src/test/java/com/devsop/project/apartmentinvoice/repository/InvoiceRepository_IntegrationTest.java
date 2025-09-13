package com.devsop.project.apartmentinvoice.repository;

import com.devsop.project.apartmentinvoice.entity.Invoice;
import com.devsop.project.apartmentinvoice.entity.Room;
import com.devsop.project.apartmentinvoice.entity.Tenant;
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
public class InvoiceRepository_IntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    @DisplayName("Should find an invoice by room, year, and month")
    void whenFindByRoomAndBillingPeriod_thenReturnInvoice() {
        // --- Setup moved directly into the test ---
        // 1. Create and save a Tenant
        Tenant testTenant = new Tenant();
        testTenant.setName("Bob Builder");
        entityManager.persist(testTenant);

        // 2. Create and save a Room
        Room testRoom = new Room();
        testRoom.setNumber(303);
        entityManager.persist(testRoom);

        // 3. Create and save the Invoice to be tested
        Invoice invoice = new Invoice();
        invoice.setRoom(testRoom);
        invoice.setTenant(testTenant);
        invoice.setBillingYear(2025);
        invoice.setBillingMonth(9);
        invoice.setIssueDate(LocalDate.of(2025, 9, 5));
        invoice.setDueDate(LocalDate.of(2025, 10, 5));
        invoice.setRentBaht(new BigDecimal("7500"));
        entityManager.persist(invoice);

        // Ensure all data is written to the test database before querying
        entityManager.flush();

        // --- Act ---
        // 4. Call the repository method we want to test
        Optional<Invoice> foundInvoice = invoiceRepository.findFirstByRoom_IdAndBillingYearAndBillingMonth(
                testRoom.getId(), 2025, 9
        );

        // --- Assert ---
        // 5. Check that the result is correct
        assertThat(foundInvoice).isPresent();
        assertThat(foundInvoice.get().getRentBaht()).isEqualByComparingTo("7500");
    }
}

