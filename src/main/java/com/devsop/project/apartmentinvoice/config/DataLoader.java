package com.devsop.project.apartmentinvoice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.devsop.project.apartmentinvoice.entity.*;
import com.devsop.project.apartmentinvoice.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepo;
    private final RoomRepository roomRepo;
    private final LeaseRepository leaseRepo;
    private final InvoiceRepository invoiceRepo;
    private final MaintenanceRepository maintenanceRepo;
    private final SupplyRepository supplyRepo;
    private final InvoiceSettingsRepository invoiceSettingsRepo;

    private final Random random = new Random(42); // Fixed seed for reproducibility

    // English names for tenants
    private static final String[] FIRST_NAMES = {
        "John", "Jane", "Mike", "Sarah", "Tom", "Emma", "David", "Lisa",
        "Chris", "Amy", "Kevin", "Laura", "Brian", "Emily", "Robert", "Anna",
        "James", "Maria", "William", "Patricia", "Daniel", "Jennifer", "Michael", "Linda",
        "Richard", "Elizabeth", "Joseph", "Susan", "Thomas", "Jessica", "Charles", "Karen",
        "Christopher", "Nancy", "Matthew", "Betty", "Anthony", "Helen", "Mark", "Sandra",
        "Donald", "Dorothy", "Steven"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White",
        "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young",
        "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
        "Green", "Adams", "Nelson"
    };

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");

        // Create user accounts
        createUsers();

        // Check if we already have data
        if (roomRepo.count() > 0) {
            log.info("Data already exists, skipping initialization");
            return;
        }

        // Create invoice settings
        createInvoiceSettings();

        // Create tenants
        List<Tenant> tenants = createTenants(42);
        log.info("Created {} tenants", tenants.size());

        // Create rooms (60 rooms: 15 per floor, floors 2-5)
        List<Room> rooms = createRooms(tenants);
        log.info("Created {} rooms", rooms.size());

        // Create leases for occupied rooms
        List<Lease> leases = createLeases(rooms);
        log.info("Created {} leases", leases.size());

        // Create invoices (10 per occupied room)
        List<Invoice> invoices = createInvoices(rooms);
        log.info("Created {} invoices", invoices.size());

        // Create maintenance records (5 per room)
        List<Maintenance> maintenances = createMaintenanceRecords(rooms);
        log.info("Created {} maintenance records", maintenances.size());

        // Create supply inventory
        List<Supply> supplies = createSupplyInventory();
        log.info("Created {} supply items", supplies.size());

        // Create special test room 1101 with overdue invoices
        createRoom1101WithOverdueInvoices();

        log.info("Data initialization complete!");
        log.info("Summary: {} rooms, {} tenants, {} leases, {} invoices, {} maintenance records, {} supplies",
                rooms.size(), tenants.size(), leases.size(), invoices.size(), maintenances.size(), supplies.size());
    }

    private void createUsers() {
        // Create admin user if not exists
        if (userRepo.findByUsername("admin").isEmpty()) {
            UserAccount admin = new UserAccount();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("1234"));
            admin.setRole("ADMIN");
            userRepo.save(admin);
            log.info("Created default admin user: admin/1234");
        }

        // Create staff user if not exists
        if (userRepo.findByUsername("staff").isEmpty()) {
            UserAccount staff = new UserAccount();
            staff.setUsername("staff");
            staff.setPassword(passwordEncoder.encode("staff123"));
            staff.setRole("STAFF");
            userRepo.save(staff);
            log.info("Created default staff user: staff/staff123");
        }
    }

    private void createInvoiceSettings() {
        if (invoiceSettingsRepo.count() == 0) {
            InvoiceSettings settings = new InvoiceSettings();
            // Don't set ID - let it be auto-generated
            settings.setPaymentDescription("Bank Transfer: SCB Account 123-456-7890\nPromptPay: 0812345678\nPlease include your room number in the transfer note.");
            settings.setInterestRatePerMonth(BigDecimal.valueOf(1.5));
            invoiceSettingsRepo.save(settings);
            log.info("Created default invoice settings");
        }
    }

    private List<Tenant> createTenants(int count) {
        List<Tenant> tenants = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String name = firstName + " " + lastName;
            String phone = String.format("08%08d", 10000000 + i);
            String lineId = (firstName + lastName).toLowerCase() + (i + 1);

            Tenant tenant = new Tenant();
            tenant.setName(name);
            tenant.setPhone(phone);
            tenant.setLineId(lineId);
            tenants.add(tenantRepo.save(tenant));
        }
        return tenants;
    }

    private List<Room> createRooms(List<Tenant> tenants) {
        List<Room> rooms = new ArrayList<>();
        int tenantIndex = 0;

        // Create 15 rooms per floor, floors 2-5 (60 rooms total)
        for (int floor = 2; floor <= 5; floor++) {
            for (int roomNum = 1; roomNum <= 15; roomNum++) {
                int roomNumber = floor * 100 + roomNum;

                Room room = new Room();
                room.setNumber(roomNumber);
                room.setCommonFeeBaht(BigDecimal.valueOf(200));
                room.setGarbageFeeBaht(BigDecimal.valueOf(100));

                // 70% occupancy rate (42 out of 60 rooms occupied)
                boolean isOccupied = tenantIndex < tenants.size() && random.nextDouble() < 0.7;
                if (isOccupied) {
                    room.setStatus("OCCUPIED");
                    room.setTenant(tenants.get(tenantIndex));
                    tenantIndex++;
                } else {
                    room.setStatus("FREE");
                    room.setTenant(null);
                }

                rooms.add(roomRepo.save(room));
            }
        }
        return rooms;
    }

    private List<Lease> createLeases(List<Room> rooms) {
        List<Lease> leases = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (Room room : rooms) {
            if (room.getTenant() != null) {
                // Random start date in 2024
                LocalDate startDate = LocalDate.of(2024, random.nextInt(12) + 1, random.nextInt(28) + 1);
                // End date 12 months later
                LocalDate endDate = startDate.plusYears(1);

                // Monthly rent between 2500-4500 THB
                BigDecimal monthlyRent = BigDecimal.valueOf(2500 + random.nextInt(2001));
                monthlyRent = monthlyRent.divide(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(100)); // Round to nearest 100

                Lease lease = new Lease();
                lease.setRoom(room);
                lease.setTenant(room.getTenant());
                lease.setStartDate(startDate);
                lease.setEndDate(endDate);
                lease.setMonthlyRent(monthlyRent);
                lease.setDepositBaht(monthlyRent); // Deposit = 1 month rent
                lease.setStatus(Lease.Status.ACTIVE);
                lease.setSettled(false);

                leases.add(leaseRepo.save(lease));
            }
        }
        return leases;
    }

    private List<Invoice> createInvoices(List<Room> rooms) {
        List<Invoice> invoices = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (Room room : rooms) {
            if (room.getTenant() != null) {
                List<Lease> activeLeases = leaseRepo.findByRoom_IdAndStatus(room.getId(), Lease.Status.ACTIVE);
                if (activeLeases.isEmpty()) continue;
                Lease lease = activeLeases.get(0);

                // Create 10 invoices per room (last 10 months)
                for (int monthsAgo = 9; monthsAgo >= 0; monthsAgo--) {
                    LocalDate billingDate = now.minusMonths(monthsAgo);
                    int billingYear = billingDate.getYear();
                    int billingMonth = billingDate.getMonthValue();

                    Invoice invoice = new Invoice();
                    invoice.setRoom(room);
                    invoice.setTenant(room.getTenant());
                    invoice.setBillingYear(billingYear);
                    invoice.setBillingMonth(billingMonth);
                    invoice.setIssueDate(billingDate.withDayOfMonth(1));
                    invoice.setDueDate(billingDate.withDayOfMonth(10));

                    // Rent
                    invoice.setRentBaht(lease.getMonthlyRent());

                    // Electricity (100-300 units, 5 THB per unit)
                    BigDecimal elecUnits = BigDecimal.valueOf(100 + random.nextInt(201));
                    BigDecimal elecRate = BigDecimal.valueOf(5);
                    invoice.setElectricityUnits(elecUnits);
                    invoice.setElectricityRate(elecRate);
                    invoice.setElectricityBaht(elecUnits.multiply(elecRate));

                    // Water (10-40 units, 18 THB per unit)
                    BigDecimal waterUnits = BigDecimal.valueOf(10 + random.nextInt(31));
                    BigDecimal waterRate = BigDecimal.valueOf(18);
                    invoice.setWaterUnits(waterUnits);
                    invoice.setWaterRate(waterRate);
                    invoice.setWaterBaht(waterUnits.multiply(waterRate));

                    // Common fees
                    invoice.setCommonFeeBaht(room.getCommonFeeBaht());
                    invoice.setGarbageFeeBaht(room.getGarbageFeeBaht());

                    // Other fees (occasionally)
                    invoice.setOtherBaht(random.nextDouble() < 0.2 ? BigDecimal.valueOf(200 + random.nextInt(801)) : BigDecimal.ZERO);

                    // Maintenance (occasionally)
                    invoice.setMaintenanceBaht(random.nextDouble() < 0.15 ? BigDecimal.valueOf(300 + random.nextInt(701)) : BigDecimal.ZERO);

                    // Calculate total
                    BigDecimal total = invoice.getRentBaht()
                            .add(invoice.getElectricityBaht())
                            .add(invoice.getWaterBaht())
                            .add(invoice.getCommonFeeBaht())
                            .add(invoice.getGarbageFeeBaht())
                            .add(invoice.getOtherBaht())
                            .add(invoice.getMaintenanceBaht());

                    invoice.setTotalBaht(total);

                    // Status based on age
                    if (monthsAgo >= 2) {
                        // Old invoices are mostly paid
                        if (random.nextDouble() < 0.95) {
                            invoice.setStatus(Invoice.Status.PAID);
                            invoice.setPaidDate(invoice.getDueDate().plusDays(random.nextInt(10)));
                        } else {
                            invoice.setStatus(Invoice.Status.OVERDUE);
                            // Add accumulated debt and interest
                            long monthsOverdue = monthsAgo - 1;
                            BigDecimal interest = total.multiply(BigDecimal.valueOf(1.5))
                                    .multiply(BigDecimal.valueOf(monthsOverdue))
                                    .divide(BigDecimal.valueOf(100));
                            invoice.setPreviousBalance(total);
                            invoice.setInterestCharge(interest);
                            invoice.setTotalBaht(total.add(total).add(interest)); // Previous + current + interest
                        }
                    } else if (monthsAgo == 1) {
                        // Last month: mix of paid/pending/overdue
                        double rand = random.nextDouble();
                        if (rand < 0.7) {
                            invoice.setStatus(Invoice.Status.PAID);
                            invoice.setPaidDate(invoice.getDueDate().plusDays(random.nextInt(5)));
                        } else if (rand < 0.9) {
                            invoice.setStatus(Invoice.Status.PENDING);
                        } else {
                            invoice.setStatus(Invoice.Status.OVERDUE);
                        }
                    } else {
                        // Current month: mostly pending
                        invoice.setStatus(Invoice.Status.PENDING);
                    }

                    invoices.add(invoiceRepo.save(invoice));
                }
            }
        }
        return invoices;
    }

    private List<Maintenance> createMaintenanceRecords(List<Room> rooms) {
        List<Maintenance> maintenances = new ArrayList<>();
        LocalDate now = LocalDate.now();

        String[] descriptions = {
            "Light bulb replacement needed",
            "Air conditioner maintenance and cleaning",
            "Plumbing repair - leaking faucet",
            "Door lock repair",
            "Window screen replacement",
            "Painting touch-up for walls",
            "Ceiling fan repair",
            "Electrical outlet repair",
            "Bathroom tile repair",
            "Water heater maintenance"
        };

        int todayCount = 0;
        int next3DaysCount = 0;

        for (Room room : rooms) {
            // Create 5 maintenance records per room
            for (int i = 0; i < 5; i++) {
                Maintenance maintenance = new Maintenance();
                maintenance.setRoom(room);
                maintenance.setDescription(descriptions[random.nextInt(descriptions.length)]);

                // Determine status and dates
                double statusRand = random.nextDouble();
                if (statusRand < 0.80) {
                    // 80% completed (past dates)
                    maintenance.setStatus(Maintenance.Status.COMPLETED);
                    maintenance.setScheduledDate(now.minusDays(random.nextInt(180) + 1));
                    maintenance.setCompletedDate(maintenance.getScheduledDate().plusDays(random.nextInt(5)));
                    maintenance.setCostBaht(BigDecimal.valueOf(200 + random.nextInt(1801)));
                } else if (statusRand < 0.93) {
                    // 13% in progress (recent dates)
                    maintenance.setStatus(Maintenance.Status.IN_PROGRESS);
                    maintenance.setScheduledDate(now.minusDays(random.nextInt(7)));
                    maintenance.setCostBaht(BigDecimal.valueOf(300 + random.nextInt(1701)));
                } else {
                    // 7% planned (future dates)
                    maintenance.setStatus(Maintenance.Status.PLANNED);

                    // Ensure we have 4 for today and 3 for next 3 days
                    if (todayCount < 4) {
                        maintenance.setScheduledDate(now);
                        todayCount++;
                    } else if (next3DaysCount < 3) {
                        maintenance.setScheduledDate(now.plusDays(1 + next3DaysCount));
                        next3DaysCount++;
                    } else {
                        maintenance.setScheduledDate(now.plusDays(random.nextInt(30) + 1));
                    }
                    maintenance.setCostBaht(BigDecimal.valueOf(400 + random.nextInt(1601)));
                }

                maintenances.add(maintenanceRepo.save(maintenance));
            }
        }

        // Ensure we have exactly 4 for today and 3 for next 3 days if not reached
        while (todayCount < 4 && !maintenances.isEmpty()) {
            Maintenance m = maintenances.get(random.nextInt(maintenances.size()));
            if (m.getStatus() == Maintenance.Status.PLANNED && !m.getScheduledDate().equals(now)) {
                m.setScheduledDate(now);
                maintenanceRepo.save(m);
                todayCount++;
            }
        }

        while (next3DaysCount < 3 && !maintenances.isEmpty()) {
            Maintenance m = maintenances.get(random.nextInt(maintenances.size()));
            if (m.getStatus() == Maintenance.Status.PLANNED &&
                m.getScheduledDate().isAfter(now.plusDays(3))) {
                m.setScheduledDate(now.plusDays(1 + next3DaysCount));
                maintenanceRepo.save(m);
                next3DaysCount++;
            }
        }

        return maintenances;
    }

    private List<Supply> createSupplyInventory() {
        List<Supply> supplies = new ArrayList<>();

        String[][] items = {
            {"Light Bulb", "2"},           // Low stock
            {"Faucet", "1"},               // Low stock
            {"Chair", "15"},
            {"Table", "8"},
            {"Refrigerator", "5"},
            {"Air Conditioner", "6"},
            {"Bed Frame", "10"},
            {"Mattress", "12"},
            {"Curtain", "20"},
            {"Door Lock", "7"}
        };

        for (String[] item : items) {
            Supply supply = new Supply();
            supply.setSupplyName(item[0]);
            supply.setSupplyAmount(Integer.parseInt(item[1]));
            supplies.add(supplyRepo.save(supply));
        }

        return supplies;
    }

    /**
     * Create special test room 1101 with Thai tenant and overdue invoices
     * to demonstrate interest calculation and debt accumulation
     */
    private void createRoom1101WithOverdueInvoices() {
        log.info("Creating Room 1101 with overdue invoices...");

        // 1. Create Room 1101 (11th floor)
        Room room1101 = new Room();
        room1101.setNumber(1101);
        room1101.setStatus("OCCUPIED");
        room1101.setCommonFeeBaht(BigDecimal.valueOf(100.00));
        room1101.setGarbageFeeBaht(BigDecimal.valueOf(50.00));

        // 2. Create Thai tenant
        Tenant thaiTenant = new Tenant();
        thaiTenant.setName("สมชาย ใจดี"); // Thai name
        thaiTenant.setPhone("081-234-5678");
        thaiTenant.setLineId("@somchai.th");
        thaiTenant = tenantRepo.save(thaiTenant);

        // 3. Assign tenant to room
        room1101.setTenant(thaiTenant);
        room1101 = roomRepo.save(room1101);

        // 4. Create active lease
        Lease lease1101 = new Lease();
        lease1101.setRoom(room1101);
        lease1101.setTenant(thaiTenant);
        lease1101.setStartDate(LocalDate.now().minusMonths(6));
        lease1101.setEndDate(LocalDate.now().plusMonths(6));
        lease1101.setMonthlyRent(BigDecimal.valueOf(8000.00));
        lease1101.setDepositBaht(BigDecimal.valueOf(16000.00));
        lease1101.setStatus(Lease.Status.ACTIVE);
        lease1101.setSettled(false);
        leaseRepo.save(lease1101);

        // 5. Invoice #1: Overdue (2 months ago, 60+ days overdue)
        LocalDate overdueIssueDate = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        LocalDate overdueDueDate = overdueIssueDate.plusDays(10); // Due 2 months ago

        Invoice overdueInvoice = new Invoice();
        overdueInvoice.setRoom(room1101);
        overdueInvoice.setTenant(thaiTenant);
        overdueInvoice.setBillingYear(overdueIssueDate.getYear());
        overdueInvoice.setBillingMonth(overdueIssueDate.getMonthValue());
        overdueInvoice.setIssueDate(overdueIssueDate);
        overdueInvoice.setDueDate(overdueDueDate);
        overdueInvoice.setRentBaht(BigDecimal.valueOf(8000.00));
        overdueInvoice.setElectricityUnits(BigDecimal.valueOf(150));
        overdueInvoice.setElectricityRate(BigDecimal.valueOf(5.50));
        overdueInvoice.setElectricityBaht(BigDecimal.valueOf(825.00));
        overdueInvoice.setWaterUnits(BigDecimal.valueOf(15));
        overdueInvoice.setWaterRate(BigDecimal.valueOf(18.00));
        overdueInvoice.setWaterBaht(BigDecimal.valueOf(270.00));
        overdueInvoice.setCommonFeeBaht(BigDecimal.valueOf(100.00));
        overdueInvoice.setGarbageFeeBaht(BigDecimal.valueOf(50.00));
        overdueInvoice.setOtherBaht(BigDecimal.ZERO);

        BigDecimal originalTotal = overdueInvoice.getRentBaht()
            .add(overdueInvoice.getElectricityBaht())
            .add(overdueInvoice.getWaterBaht())
            .add(overdueInvoice.getCommonFeeBaht())
            .add(overdueInvoice.getGarbageFeeBaht());

        // Calculate interest: 2 months overdue × 1.5% per month
        long monthsOverdue = 2;
        BigDecimal interestRate = BigDecimal.valueOf(0.015); // 1.5%
        BigDecimal interest = originalTotal.multiply(interestRate)
            .multiply(BigDecimal.valueOf(monthsOverdue))
            .setScale(2, java.math.RoundingMode.HALF_UP);

        overdueInvoice.setPreviousBalance(BigDecimal.ZERO);
        overdueInvoice.setInterestCharge(interest);
        overdueInvoice.setTotalBaht(originalTotal.add(interest));
        overdueInvoice.setStatus(Invoice.Status.OVERDUE);
        overdueInvoice.setPaidDate(null);
        overdueInvoice = invoiceRepo.save(overdueInvoice);

        log.info("Created overdue invoice #{} for Room 1101: Original {} + Interest {} = Total {}",
                overdueInvoice.getId(), originalTotal, interest, overdueInvoice.getTotalBaht());

        // 6. Invoice #2: Current month (pending, not overdue yet, but carries previous balance)
        LocalDate currentIssueDate = LocalDate.now().withDayOfMonth(1);
        LocalDate currentDueDate = currentIssueDate.plusDays(10); // Due in ~10 days

        Invoice currentInvoice = new Invoice();
        currentInvoice.setRoom(room1101);
        currentInvoice.setTenant(thaiTenant);
        currentInvoice.setBillingYear(currentIssueDate.getYear());
        currentInvoice.setBillingMonth(currentIssueDate.getMonthValue());
        currentInvoice.setIssueDate(currentIssueDate);
        currentInvoice.setDueDate(currentDueDate);
        currentInvoice.setRentBaht(BigDecimal.valueOf(8000.00));
        currentInvoice.setElectricityUnits(BigDecimal.valueOf(145));
        currentInvoice.setElectricityRate(BigDecimal.valueOf(5.50));
        currentInvoice.setElectricityBaht(BigDecimal.valueOf(797.50));
        currentInvoice.setWaterUnits(BigDecimal.valueOf(14));
        currentInvoice.setWaterRate(BigDecimal.valueOf(18.00));
        currentInvoice.setWaterBaht(BigDecimal.valueOf(252.00));
        currentInvoice.setCommonFeeBaht(BigDecimal.valueOf(100.00));
        currentInvoice.setGarbageFeeBaht(BigDecimal.valueOf(50.00));
        currentInvoice.setOtherBaht(BigDecimal.ZERO);
        currentInvoice.setPreviousBalance(overdueInvoice.getTotalBaht()); // Carry forward overdue balance
        currentInvoice.setInterestCharge(BigDecimal.ZERO);

        BigDecimal currentMonthCharges = currentInvoice.getRentBaht()
            .add(currentInvoice.getElectricityBaht())
            .add(currentInvoice.getWaterBaht())
            .add(currentInvoice.getCommonFeeBaht())
            .add(currentInvoice.getGarbageFeeBaht());

        currentInvoice.setTotalBaht(currentMonthCharges.add(currentInvoice.getPreviousBalance()));
        currentInvoice.setStatus(Invoice.Status.PENDING);
        currentInvoice.setPaidDate(null);
        currentInvoice = invoiceRepo.save(currentInvoice);

        log.info("Created current invoice #{} for Room 1101: Current {} + Previous Balance {} = Total {}",
                currentInvoice.getId(), currentMonthCharges, currentInvoice.getPreviousBalance(), currentInvoice.getTotalBaht());

        log.info("✅ Room 1101 setup complete - Thai tenant '{}' with 1 overdue invoice (with interest) and 1 current invoice (with previous balance)",
                thaiTenant.getName());
    }
}
