package com.devsop.project.apartmentinvoice.metrics;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class InvoiceMetrics {

  private final Timer importTimer;
  private final Counter importErrorCounter;
  private final Counter invoiceCreatedCounter;

  public InvoiceMetrics(MeterRegistry registry) {
    this.importTimer = Timer.builder("apartment.invoice.import.latency")
      .description("Time spent importing invoices from CSV files")
      .publishPercentileHistogram()
      .register(registry);

    this.importErrorCounter = Counter.builder("apartment.invoice.import.errors")
      .description("Number of invoice import errors")
      .register(registry);

    this.invoiceCreatedCounter = Counter.builder("apartment.invoice.generated")
      .description("Total invoices created via CSV import")
      .register(registry);
  }

  public <T> T recordImport(Supplier<T> action) {
    return importTimer.record(action);
  }

  public void incrementImportErrors() {
    importErrorCounter.increment();
  }

  public void incrementInvoiceCreated() {
    invoiceCreatedCounter.increment();
  }
}
