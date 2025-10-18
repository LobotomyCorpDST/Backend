package com.devsop.project.apartmentinvoice.unit.controller;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class LeaseControllerUnitTest {
    @Mock
    private com.devsop.project.apartmentinvoice.service.LeaseService leaseService;

    @InjectMocks
    private com.devsop.project.apartmentinvoice.controller.LeaseController controller;

    public LeaseControllerUnitTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSomething() {
        assertNotNull(controller);
    }
}
