package com.devsop.project.apartmentinvoice.unit.controller;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;   
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class TenantControllerUnitTest {


    @InjectMocks
    private com.devsop.project.apartmentinvoice.controller.TenantController controller;

    public TenantControllerUnitTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSomething() {
        assertNotNull(controller);
    }
}
