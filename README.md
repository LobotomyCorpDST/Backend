# Apartment Invoice (Spring Boot + React)

## Prerequisites

- **Java 17+**
- **Node.js 18+** และ **npm**
- พอร์ตว่าง: Backend `8080`, Frontend `3000`

---

## การเชื่อม DB

แก้ไฟล์ `src/main/resources/application.yaml` ให้ตรงโปรไฟล์ที่ต้องการ

### โปรไฟล์ `mysql`

```yaml
spring:
  config:
    activate:
      on-profile: mysql
  datasource:
    url: jdbc:mysql://localhost:3306/apartment_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root        # ← เปลี่ยน
    password: admin123    # ← เปลี่ยน
  jpa:
    hibernate:
      ddl-auto: update

```

รันด้วย:

```bash
./gradlew bootRun --args="--spring.profiles.active=mysql"

```

## การรัน Backend + Frontend

### 1) Backend (โปรไฟล์ `dev` ใช้ H2 in-memory)

```bash
./gradlew bootRun --args="--spring.profiles.active=mysql"

```

> ถ้าเปิด H2 console ไว้แล้ว: http://localhost:8080/h2-console
> 

### 2) Frontend

สร้างไฟล์ `Frontend/app/.env` (อย่าคอมมิต)

```
REACT_APP_API_BASE=http://localhost:8080

```

รัน

```bash
npm start

```

---

---

## เส้นทางหน้า UI (Frontend)

- `/` → หน้า Login (mock)
- `/home` → หน้า Home (รายการห้อง + ประวัติใบแจ้งหนี้)
- `/room-details/:roomNumber`
    - แท็บ **ใบแจ้งหนี้**: สร้าง/ดู/พิมพ์/ดาวน์โหลด PDF, Mark Paid/Unpaid
    - แท็บ **บำรุงรักษา**: เพิ่มงานใหม่, ทำเสร็จ (Completed), แสดงจาก backend จริง

> สำหรับ seed/เดโม ปัจจุบันแม็ป roomNumber → roomId เป็น 101→1, 102→2, 201→3
> 

---

## API สรุป (Backend)

### Invoice

- **สร้างใบแจ้งหนี้**
    
    ```
    POST /api/invoices?includeCommonFee={bool}&includeGarbageFee={bool}
    Content-Type: application/json
    
    ```
    
    ตัวอย่าง Body:
    
    ```json
    {
      "roomId": 1,
      "billingYear": 2025,
      "billingMonth": 9,
      "issueDate": "2025-09-04",
      "dueDate": "2025-09-11",
      "electricityUnits": 165,
      "electricityRate": 6.5,
      "waterUnits": 12,
      "waterRate": 18.5,
      "otherBaht": 100
    }
    
    ```
    
- **ดูใบแจ้งหนี้ตามห้อง**
    
    ```
    GET /api/invoices/by-room/{roomId}
    
    ```
    
- **Mark เป็นจ่ายแล้ว**
    
    ```
    POST /api/invoices/{id}/mark-paid?paidDate=YYYY-MM-DD
    
    ```
    
- **ย้อนเป็นค้างชำระ (PENDING)**
    
    ```
    PATCH /api/invoices/{id}/unpaid
    
    ```
    
- **แสดงเพื่อพิมพ์ (Thymeleaf)**
    
    ```
    GET /invoices/{id}/print
    
    ```
    
- **ดาวน์โหลด/เปิด PDF**
    
    ```
    GET /invoices/{id}/pdf
    
    ```
    

> ใบแจ้งหนี้จะ รวมค่า Maintenance ของเดือนบิล เฉพาะที่ Status.COMPLETED เท่านั้น (คำนวณใน InvoiceController)
> 

---

### Maintenance

- **ดึงรายการงานของห้อง**
    
    ```
    GET /api/maintenance/by-room/{roomId}
    
    ```
    
- **สร้างงานบำรุงรักษา**
    
    ```
    POST /api/maintenance
    Content-Type: application/json
    
    ```
    
    ตัวอย่าง Body:
    
    ```json
    {
      "roomId": 1,
      "description": "ซ่อมแอร์",
      "scheduledDate": "2025-09-10",
      "costBaht": 800
    }
    
    ```
    
- **ทำเสร็จ (Completed)**
    
    ```
    PATCH /api/maintenance/{id}/complete?completedDate=YYYY-MM-DD
    
    ```
    
- **แก้ไขงาน**
    
    ```
    PUT /api/maintenance/{id}
    Content-Type: application/json
    
    ```
    

---

## วิธีทดสอบเร็วด้วย curl (ตัวอย่าง)

```bash
# 1) สร้างใบแจ้งหนี้แบบรวมค่าส่วนกลาง (ปรับ roomId/ตัวเลขตามจริง)
curl -X POST "http://localhost:8080/api/invoices?includeCommonFee=true&includeGarbageFee=false" \
  -H "Content-Type: application/json" \
  -d '{
        "roomId": 1,
        "billingYear": 2025,
        "billingMonth": 9,
        "issueDate": "2025-09-04",
        "dueDate": "2025-09-11",
        "electricityUnits": 100,
        "electricityRate": 8.0,
        "waterUnits": 3,
        "waterRate": 18.0,
        "otherBaht": 50
      }'

# 2) ดูใบแจ้งหนี้ของห้อง
curl "http://localhost:8080/api/invoices/by-room/1"

# 3) ทำงานบำรุงรักษาใหม่
curl -X POST "http://localhost:8080/api/maintenance" \
  -H "Content-Type: application/json" \
  -d '{ "roomId": 1, "description": "เปลี่ยนหลอดไฟ", "scheduledDate": "2025-09-06", "costBaht": 120 }'

# 4) ดึงรายการ Maintenance ของห้อง
curl "http://localhost:8080/api/maintenance/by-room/1"

# 5) ทำงานเป็น Completed
curl -X PATCH "http://localhost:8080/api/maintenance/1/complete?completedDate=2025-09-07"

```

---

## Tips

- **.env (Frontend) ห้ามคอมมิต** — แนะนำเพิ่ม `.env.example` แทน:
    
    ```
    REACT_APP_API_BASE=http://localhost:8080
    
    ```
    
- ถ้า Frontend เจอ CORS ให้ตรวจว่าที่ Controller หลักมี
    
    ```java
    @CrossOrigin(origins = "http://localhost:3000")
    
    ```
    
- เวลาเทสต์วันที่ ให้ใช้รูปแบบ `YYYY-MM-DD`
- ถ้าพิมพ์/เปิด PDF ไม่ขึ้น ให้ตรวจ `invoice.html` และว่า `id` ของใบแจ้งหนี้มีจริง

---

## สคริปต์ที่ใช้บ่อย

**Backend**

```bash
./gradlew bootRun
./gradlew build
./gradlew bootRun --args="--spring.profiles.active=mysql"

```

**Frontend**

```bash
cd Frontend/app
npm install
npm start
npm run build

```