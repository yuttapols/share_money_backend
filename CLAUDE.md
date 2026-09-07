# Share Money — Backend (Spring Boot)

ระบบติดตามหนี้ระหว่างบุคคล (peer lending tracker) — เอกสาร requirement/design เต็มอยู่ที่
`C:\GIT\DOCUMENT\share_money_document\docs`. ไฟล์นี้สรุปเฉพาะสิ่งที่ backend ต้องรู้และ pattern
ที่ต้องใช้ซ้ำทุกครั้งที่เขียนโค้ดใน repo นี้.

## Tech Stack

| หมวด | เลือกใช้ |
|---|---|
| Framework | Spring Boot 3.3.x, Java 21 (LTS), Maven |
| Web | Spring Web (REST Controller) |
| Security | Spring Security 6 + JWT (access + refresh), `BCryptPasswordEncoder` |
| Data Access | Spring Data JPA + Hibernate เป็นหลัก, `JdbcTemplate` เฉพาะ query ที่ซับซ้อน/report หนัก |
| Database | PostgreSQL 16 (prod/dev) |
| Migration | Flyway, versioned SQL (`V{n}__description.sql`) |
| Validation | Jakarta Bean Validation (`@Valid`, `@NotBlank`, ...) |
| Mapping | MapStruct (Entity ↔ DTO) |
| Docs | springdoc-openapi (Swagger UI) |
| Logging | SLF4J + Logback, structured log สำหรับ audit |

## Package Structure

โมดูลแบ่งตาม domain, ไม่แบ่งตาม layer ข้ามโมดูล — แต่ละโมดูลมี controller/service/repository/entity/dto ของตัวเอง

```
com.sharemoney/
  common/           response wrapper, error code, exception handler, base entity, security util, file storage abstraction
  auth/             login/refresh/logout/me/change-password, JWT issue/verify
  user/             User entity (creditor/debtor), cascade rename/delete
  menu/             menu_items/menu_permissions, sidebar tree ตาม role
  admin/            login_logs, installment choices, migration (phase หลัง)
  debt/             debts/installments/open_loan_records (phase หลัง)
  slip/             slip upload/view (phase หลัง)
  document/         document upload/download (phase หลัง)
  report/           due report + PDF (phase หลัง)
```

แต่ละโมดูลมีโครงเดียวกัน: `controller/`, `service/`, `repository/`, `entity/`, `dto/`

## Response Pattern (บังคับทุก endpoint)

ทุก endpoint คืนค่าเป็น `ApiResponse<T>` (`com.sharemoney.common.response.ApiResponse`) ห้าม controller คืน
entity/DTO ดิบ ๆ ตรง ๆ โดยไม่ห่อ

**สำเร็จ**
```json
{
  "status": "C",
  "errorCode": "0000",
  "errorDesc": "SUCCESS",
  "displayMessage": "Data has been saved successfully.",
  "data": { }
}
```
สร้างด้วย `ApiResponse.success(data)` หรือ `ApiResponse.success(data, "ข้อความเฉพาะ endpoint นี้")`

**Error จาก business validation** (`BusinessException` + `ErrorCode` enum กลาง — ดู `common/error/ErrorCode.java`)
```json
{
  "status": "E",
  "errorCode": "ERR_NOT_LATEST_RECORD",
  "errorDesc": "Record is not eligible for update or deletion",
  "displayMessage": "Record is not eligible for update or deletion",
  "data": null
}
```
โยน `throw new BusinessException(ErrorCode.XXX)` จาก service layer เท่านั้น — ห้าม throw จาก controller,
`GlobalExceptionHandler` จะจับแล้วแปลงเป็น response ให้อัตโนมัติ ทุก error code ใหม่ต้องเพิ่มเป็น enum
constant ใน `ErrorCode` ห้าม hardcode string กระจายอยู่หลายที่

**Error จาก system/framework** (route ไม่พบ, token ไม่ถูกต้อง, ไม่มีสิทธิ์ระดับ filter, uncaught exception)
```json
{
  "status": "E",
  "errorCode": "404",
  "errorDesc": "ScenarioId is unknown or not found",
  "displayMessage": "ScenarioId is unknown or not found",
  "data": null
}
```
`errorCode` = HTTP status ตัวเลขเป็น string, จับที่ `GlobalExceptionHandler` เดียวกัน — ไม่ต้องสร้าง
`ErrorCode` enum ใหม่สำหรับกรณีนี้ ใช้ helper `ApiResponse.systemError(HttpStatus, message)`

กติกาเลือกว่า error ไหนควรเป็นแบบไหน: ถ้าเป็น **กติกาทางธุรกิจที่ตั้งชื่อได้** (username ซ้ำ, ไม่ใช่เจ้าของ,
สถานะไม่ถูกต้องสำหรับ action นี้) → business error code เสมอ. ถ้าเป็น **ปัญหาระดับ infrastructure/ระบบ**
(auth ไม่ผ่าน, ไม่พบ route, exception ที่ไม่คาดคิด) → system error ใช้ HTTP status ตรง ๆ

## Shared / Common Code — ต้องใช้ของกลาง ห้ามเขียนซ้ำ

| ต้องการ | ใช้ |
|---|---|
| ห่อ response | `ApiResponse<T>` |
| โยน business error | `BusinessException` + `ErrorCode` |
| แปลง exception → response | `GlobalExceptionHandler` (`@RestControllerAdvice`) |
| ผู้ใช้ที่ login อยู่ / role / ownership check | `SecurityUtils` |
| audit log (LOGIN/LOGOUT/UPLOAD_SLIP ฯลฯ) | `AuditLogService` |
| อัปโหลด/เก็บไฟล์ (slip, document, avatar) | `FileStorageService` (interface เดียว, impl `CloudinaryFileStorageService` — ดูหัวข้อ "File Storage — Cloudinary") |
| created_at/updated_at | extend `BaseEntity` + JPA Auditing (`@CreatedDate`/`@LastModifiedDate`) |
| แปลง Entity ↔ DTO | MapStruct mapper interface ต่อโมดูล ห้ามแปลง manual ด้วยมือถ้า mapping ตรงไปตรงมา |

ฟังก์ชันไหนถูกใช้ซ้ำเกิน 1 โมดูล ให้ย้ายเข้า `common/` ทันที ไม่ปล่อยให้ copy-paste

## Security & Data Rules

- Query ทุกจุดใช้ JPA method/`@Query` แบบ parameter binding หรือ `JdbcTemplate` แบบ named/positional
  parameter เท่านั้น — **ห้าม string concatenation ต่อ SQL เด็ดขาด**
- ป้องกัน N+1 เสมอ: ใช้ `JOIN FETCH` หรือ `@EntityGraph` เมื่อดึง entity พร้อม association, ใช้
  `@BatchSize`/`fetch = FetchType.LAZY` เป็นค่าเริ่มต้น แล้ว fetch ตามจริงเป็นจุด ๆ ไป
  Query ที่ report/list ใหญ่ (เช่น due report, login logs) พิจารณาใช้ `JdbcTemplate` + projection แทน
  entity graph ถ้า JPA ทำให้ query ซับซ้อนเกินจำเป็น
- ทุก endpoint บังคับ role ด้วย `@PreAuthorize("hasRole('...')")` และตรวจ ownership เพิ่มที่ service layer
  เสมอ (เช่น creditor แก้ได้เฉพาะ debtor/debt ของตัวเอง) — ห้ามพึ่ง `@PreAuthorize` อย่างเดียว
- ทุก field ที่รับจาก client ต้อง validate ทั้ง shape (`@NotBlank`, `@Size`, `@Pattern`, `@DecimalMin`
  ตาม `07-validation-rules.md`) และ business rule (unique, ownership, referential) ที่ service layer —
  backend คือ source of truth สุดท้ายเสมอ ไม่เชื่อ validation ฝั่ง frontend
- ไฟล์อัปโหลด (slip/document/avatar) ต้อง validate content-type จริงจาก header (ไม่เชื่อ extension) และ
  ขนาดไฟล์ที่ backend เสมอ
- การอัปเดตที่แตะ denormalized cache field (`debts.paid_amount`, `debts.status`) ต้องอยู่ใน
  `@Transactional` เดียวกับการเขียน child record เสมอ
- Password เก็บด้วย BCrypt เท่านั้น, JWT access token อายุสั้น (~30 นาที) + refresh token เก็บใน DB
  (hash) เพื่อ revoke ได้
- Login ผิดครบ 5 ครั้งต่อ username → lock ชั่วคราว 15 นาที (`LoginAttemptService`, in-memory ต่อ instance —
  ถ้า scale เป็นหลาย instance ต้องย้ายไป shared store เช่น Redis)
- `JwtSecretGuard` เช็คตอน startup ว่าถ้า profile ≠ `dev` ห้ามใช้ `JWT_SECRET` ค่า default เด็ดขาด (fail fast)

## Code Style

- ห้ามเขียน comment ในโค้ด ยกเว้นกรณีอธิบาย constraint/เหตุผลที่ไม่ obvious จริง ๆ
- ไม่เพิ่ม abstraction/validation/error handling เกินกว่าที่ requirement ต้องการ
- Format โค้ดให้อ่านง่าย, ตั้งชื่อสื่อความหมาย, 1 class ทำหน้าที่เดียว

## File Storage — Cloudinary (ใช้ตอน Phase 3: slip / document / avatar)

ตัดสินใจแล้วว่าใช้ **Cloudinary** เป็น image/file storage แทน local disk / S3-MinIO ที่ระบุไว้ในเอกสาร
`05-tech-stack.md` เดิม — ตอนเริ่ม Phase 3 ให้ทำตามนี้ ไม่ต้องออกแบบใหม่:

**Dependency**: `com.cloudinary:cloudinary-http45` ใน `pom.xml`
**Config**: `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` เป็น env var เท่านั้น
ห้าม hardcode ใน `application.yml`

**DB columns มาตรฐาน** — ใช้ชุดนี้กับทุกตาราง/field ที่เก็บไฟล์ผ่าน Cloudinary (`slips`, `documents`,
`users` avatar) แทน `storage_key`/`url`/`content_type` แบบเดิมใน `03-database-design.md`:

| Column | ใช้ทำอะไร |
|---|---|
| `public_id` | id ที่ Cloudinary ใช้อ้างอิงไฟล์ — ใช้ตอน delete และ generate URL ใหม่ |
| `secure_url` | https URL ใช้แสดงผล/ดาวน์โหลดตรง ๆ (เฉพาะกรณี public delivery) |
| `original_filename` | ชื่อไฟล์ต้นฉบับที่ user อัปโหลด |
| `format` | นามสกุลจริงจาก Cloudinary response (jpg/png/pdf) |
| `resource_type` | `image` หรือ `raw` — Cloudinary ต้องใช้ค่านี้ตอน delete ด้วย ขาดไม่ได้ |
| `bytes` | ขนาดไฟล์ |
| `uploaded_at` | เวลาอัปโหลด |

`users.avatar_key` ที่มีอยู่แล้วใน `V1__init_schema.sql` (ยังไม่เคย apply กับ DB จริง) ให้แก้เป็น
`avatar_public_id` + `avatar_url` ตอนเริ่ม Phase 3 แทนที่จะเพิ่ม migration ใหม่

**Security**: สลิป/เอกสารสัญญาเป็นข้อมูลการเงินที่ต้องมี ownership check (creditor/debtor เจ้าของคู่เท่านั้น
เห็นได้) — ห้ามใช้ Cloudinary public URL ตรง ๆ เพราะใครก็เปิดดูได้ถ้ารู้ลิงก์ ให้อัปโหลดแบบ
`type: authenticated`/private แล้วให้ backend generate **signed URL อายุสั้น** ทุกครั้งที่ endpoint GET ถูก
เรียก (เช็ค ownership ที่ service layer ก่อน generate URL เสมอตามกติกา ownership เดิม) — ห้ามเก็บ signed URL
ลง DB เพราะหมดอายุ เก็บแค่ `public_id` ไว้ generate ใหม่ทุกครั้ง ส่วน avatar ไม่ sensitive เท่า ใช้ public
delivery ธรรมดาได้ ไม่ต้อง sign

**Common abstraction** (`common/storage`):
```
FileStorageService (interface)
  StoredFile upload(MultipartFile file, String folder, boolean authenticated)
  void delete(String publicId, String resourceType)
  String generateSignedUrl(String publicId, String resourceType)

CloudinaryFileStorageService implements FileStorageService
CloudinaryConfig            → @Bean Cloudinary
StoredFile record(publicId, secureUrl, originalFilename, format, resourceType, bytes)
```

**Flow**:
- **Push (upload)** — validate content-type จาก header จริง + ขนาดไฟล์ก่อนเสมอ (ตามกติกาเดิมใน
  "Security & Data Rules") → `cloudinary.uploader().upload()` → เก็บผลลัพธ์ 7 คอลัมน์ด้านบนลง DB ใน
  `@Transactional` เดียวกับ business logic (เช่น replace สลิปเก่าตาม FR-7.3)
- **Get** — avatar คืน `secure_url` ตรง ๆ, สลิป/เอกสาร generate signed URL สดใหม่ทุกครั้ง
- **Delete** — เรียก `cloudinary.uploader().destroy(publicId, resource_type)` ก่อนลบ row ใน DB เสมอ
  (กันไฟล์ orphan ค้างบน Cloudinary) — ใช้จุดนี้ทั้งตอน replace สลิปเก่า, ลบสลิป/เอกสารตรง ๆ, และตอน cascade
  ลบ debtor ที่มีสลิปอยู่

## Domain Reference (ย่อจาก requirement เดิม)

- 3 role: `ADMIN` / `CREDITOR` (เจ้าหนี้) / `DEBTOR` (ลูกหนี้) — 1 debtor ผูกกับ 1 creditor เท่านั้น
  (`users.creditor_id`)
- หนี้ 3 แบบ: `INSTALLMENT` (แบ่งจ่ายเป็นงวด), `OPEN` (กู้เปิด กรอกเอง ไม่คำนวณอัตโนมัติ), `FULL` (legacy
  จ่ายเต็มจำนวนครั้งเดียว)
- สลิปโอนเงิน: เก็บล่าสุด **3 ใบต่อคู่** (debtor × creditor) — อัปโหลดใหม่เมื่อครบ 3 ใบแล้วจะลบใบเก่าสุดทิ้งอัตโนมัติ
  (rolling window ไม่ใช่ overwrite แบบระบบเดิม), จำกัด `image/*` ≤ 1MB ต่อไฟล์
- รายละเอียด business rule แต่ละ domain ดูที่ `01-requirements.md` (FR-1 ถึง FR-11), API contract เต็มที่
  `04-api-specification.md`, ตาราง DB ที่ `03-database-design.md`, validation ทุก field ที่
  `07-validation-rules.md`

### ⚠️ Override: ขอบเขตสิทธิ์ ADMIN (ต่างจากเอกสาร requirement เดิม)

เอกสาร `01-requirements.md`/`04-api-specification.md` §15 (role matrix) ระบุว่า ADMIN เห็นข้อมูลหนี้ได้ทั้งหมด
("admin=ทั้งหมด") — **แต่ตัดสินใจเปลี่ยนแล้ว**: ข้อมูลหนี้เป็นเรื่องระหว่างเจ้าหนี้กับลูกหนี้เท่านั้น ADMIN
**ไม่ควรเห็น**

- **ADMIN ห้ามเข้าถึง**: `/api/debts/**` (รวม installment/open-loan record ทุก action), `/api/reports/due`
  และ `/api/reports/due/pdf` — ตอนสร้าง Phase 2 (debt module) และ Phase 3 (report module) ห้ามใส่
  `hasRole('ADMIN')` ใน `@PreAuthorize` ของ endpoint กลุ่มนี้เด็ดขาด (ต่างจาก role matrix ในเอกสารเดิม)
- **ADMIN ยังเข้าถึงได้**: `/api/slips/**` (ดูรูปสลิปหลักฐานการโอนได้ปกติ) — เผื่อกรณีต้อง support/ไกล่เกลี่ยข้อพิพาท
  การโอนเงินระหว่างเจ้าหนี้-ลูกหนี้
- **ADMIN ยังเข้าถึงได้ตามเดิม**: user management ทั้งหมด (creditor/debtor CRUD), เมนู, login log, ตัวเลือก
  จำนวนงวด, documents — ไม่กระทบ
- เมนู `menu.debts` และ `menu.reports` ถูกเอา ADMIN ออกจาก `menu_permissions` แล้ว (`V5__restrict_admin_debt_menu.sql`)

## Roadmap

แบ่ง 4 phase ตาม `09-implementation-roadmap.md` — Phase 1 (Foundation/Security/User) กำลังอยู่ระหว่างทำ
ใน repo นี้ ดูสถานะจริงจาก git log/commit ไม่ใช่จากไฟล์นี้ (ไฟล์นี้ไม่อัปเดตตามความคืบหน้า)

1. Foundation, Security, User — schema พื้นฐาน (users/refresh_tokens/login_logs/menu_items/menu_permissions),
   auth lifecycle, RBAC+ownership, CRUD creditor/debtor, menu API, Swagger, health endpoint
2. Debt Domain — debts/installments/open_loan_records, payment actions, denormalized totals
3. File Storage, Slip, Documents, Reports — `FileStorageService` (Cloudinary — ดูหัวข้อ "File Storage —
   Cloudinary" ด้านบน), slip/document API, due report + PDF
4. Admin, Operations, Release — installment choices ✅, docker-compose ✅, error mapping ✅, legacy data
   migration ✅ (ดูหัวข้อ "Legacy Data Migration" ด้านล่าง — ทำแบบ one-time tool ธรรมดา ไม่ใช่ full
   staging/dry-run/reconciliation pipeline ตาม `10-legacy-data-migration.md` เพราะข้อมูลจริงมีขนาดเล็ก
   ระดับ personal use ~10 user), backup/restore runbook ⏸ (รอ environment deploy จริง)

## Legacy Data Migration

ข้อมูลจริงจากระบบเดิม (Google Apps Script) อยู่ใน 1 ไฟล์ Excel export เดียว — **มี sheet ปนที่ไม่เกี่ยวกับ
Share Money เลย** (`Transactions`, `Categories`, `Budgets`, `Food`, `Wedding`) เป็น sheet จาก Google Apps
Script ตัวอื่นที่ใช้ account เดียวกัน **ห้าม migrate sheet พวกนี้เด็ดขาด**

Sheet ที่เกี่ยวข้องจริง: `Users`, `Settings` (installmentChoices), `Debts` (installments/open-records ฝังเป็น
JSON string ใน cell เดียว ตรงกับที่ improvement backlog ในเอกสารเดิมคาดไว้), `Logins` — ส่วน `Slips` (7 ใบ)
**ข้ามไปก่อนโดยตั้งใจ** เพราะไฟล์ยังอยู่บน Google Drive ไม่ใช่ Cloudinary ต้อง OAuth เข้าไปโหลดซึ่งไม่คุ้มกับ
แค่ 8 debtor — ให้ debtor อัปโหลดสลิปใหม่ผ่านระบบใหม่เองถ้ายังต้องใช้

**เครื่องมือ**: `com.sharemoney.migration.LegacyDataMigrationTool` — plain class มี `main()` ธรรมดา **ไม่ใช่**
Spring bean/CommandLineRunner (ไม่ถูกรันตอน production app start แน่นอน) รันเองครั้งเดียวผ่าน IDE หรือ
`mvn exec:java -Dexec.mainClass=com.sharemoney.migration.LegacyDataMigrationTool -Dexec.args="<path-to-xlsx>"`
ใช้ `org.apache.poi:poi-ooxml` scope `provided` (compile ได้ แต่ไม่ติดไปกับ jar ที่ deploy จริง)

- Password เดิมเป็น SHA-256+salt แปลงเป็น BCrypt ไม่ได้ (ตามที่ FR-11.6 เตือนไว้) — migrate user ทุกคนด้วย
  temp password `123456` เหมือนกันหมด ให้ไปเปลี่ยนเองผ่าน `POST /api/auth/change-password`
- ใช้ raw JDBC ตรง ๆ ไม่ผ่าน JPA/Spring context (เป็น one-time tool ไม่ต้องพึ่ง Spring boot ขึ้นมาทั้งระบบ)
- ทำในทรานแซกชันเดียว (`connection.setAutoCommit(false)`) — error จุดไหนก็ rollback ทั้งหมด ไม่ทิ้งข้อมูลค้าง
- Skip user ที่มี username ซ้ำอยู่แล้วในระบบ (รองรับ rerun โดยไม่ error ซ้ำ และจัดการ `admin` ที่ seed ไว้แล้ว
  ชนกับ `admin` ในชีตต้นทางโดยอัตโนมัติ)
- `installment_choices` ถูก update เป็นค่าจริงจากระบบเดิม (`2,3,4,5,6,9,10,12,15,18,24,30,48`) แทนค่า default
  เดิมที่ seed ไว้ (`2,3,6,9,12`) เพราะพบว่ามีการใช้ 30 งวดจริงในข้อมูล
- ตรวจสอบ parsing กับไฟล์จริงแล้วผ่านหมด (dry-run แยกนอก DB) มีจุดเดียวที่ต้องแก้: บาง debt ไม่มี `dueDate`
  ในงวดแรก ต้อง fallback ไปใช้ `payDate` ก่อนแล้วค่อย fallback เป็น `createdAt` เป็นทางเลือกสุดท้าย

## Future Considerations (ยังไม่ทำตอนนี้ แต่ควรพิจารณาทีหลัง)

- **`login_logs` retention/archive policy** — ตอนนี้เป็น append-only insert ทุกครั้งที่ login/logout ไม่มี
  การลบ ตารางจะโตไปเรื่อย ๆ ตามอายุการใช้งานระบบ ควรพิจารณาทำตอน Phase 4 (hardening/operations):
  ตั้ง retention (เช่น เก็บ 90 วันแล้วลบทิ้ง) หรือ archive ออกไปตารางแยก/storage อื่นก่อนลบ ถ้าต้องเก็บไว้ใช้
  สอบสวนย้อนหลัง
- **`ip_address` ใน `login_logs` เชื่อ `X-Forwarded-For` แบบไม่ตรวจสอบ** (`AuthController.resolveClientIp`) —
  ตอนนี้ไม่มี reverse proxy คั่นกลาง ใครก็ปลอม header นี้ใส่ IP อะไรก็ได้ผ่าน client ตรง ๆ แล้วเราบันทึกลง DB
  โดยไม่เช็ค ต้องแก้ตอน Phase 4 พร้อมกับตั้ง Nginx จริง: ตั้ง `server.forward-headers-strategy: framework`
  ของ Spring Boot + ให้ Nginx **overwrite** (ไม่ใช่แค่ pass-through) header `X-Forwarded-For` ด้วย IP จริงของ
  client เสมอ ไม่งั้นข้อมูลนี้ใช้สอบสวน incident จริงไม่ได้เลย
- **`docker-compose.yml`** — มีแค่ `Dockerfile` (multi-stage build, `maven:3.9-eclipse-temurin-21` →
  `eclipse-temurin:21-jre-alpine`, non-root user) สำหรับตัว backend เดี่ยว ๆ เท่านั้น ยังไม่มี compose ผูกกับ
  PostgreSQL/Nginx ให้ครบ — ทำตอน Phase 4 ตาม roadmap (หมายเหตุ: ไม่ต้องมี MinIO ใน compose แล้ว เพราะเปลี่ยน
  ไปใช้ Cloudinary สำหรับเก็บรูปแทน — ดูหัวข้อ "File Storage — Cloudinary")
