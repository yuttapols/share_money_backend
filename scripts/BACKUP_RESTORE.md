# Backup / Restore Runbook

ระดับข้อมูล: personal use ~10 user, ตาราง `slips`/`documents`/`users` avatar เก็บเฉพาะ metadata
(`public_id`) — ไฟล์จริงอยู่บน Cloudinary ไม่ต้อง backup แยก. runbook นี้ backup เฉพาะ PostgreSQL
(`share_money` database, schema `shmy`) ด้วย `pg_dump`/`pg_restore` ธรรมดา ไม่ใช้ replication/PITR
เพราะเกินความจำเป็นสำหรับขนาดข้อมูลนี้.

## Backup

```bash
./scripts/backup.sh
```

- ใช้ `pg_dump -F c` (custom format, compressed, restore ได้แบบ selective) ผ่าน `docker exec` เข้า
  container postgres ของ `docker-compose.yml`
- ไฟล์ออกที่ `./backups/share_money_<timestamp>.dump`
- ลบไฟล์เก่ากว่า `BACKUP_RETENTION_DAYS` (default 30 วัน) อัตโนมัติทุกครั้งที่รัน
- Override ได้ผ่าน env var: `BACKUP_DIR`, `BACKUP_RETENTION_DAYS`, `POSTGRES_CONTAINER`, `DB_USERNAME`, `DB_NAME`

### ตั้งเป็น scheduled job (cron ตัวอย่าง — รันทุกวันตี 3)

```
0 3 * * * cd /path/to/share_money_backend && ./scripts/backup.sh >> /var/log/share_money_backup.log 2>&1
```

เก็บไฟล์ `.dump` ไว้นอกเครื่อง host เดียวกับ DB ด้วย (เช่น sync ไป object storage/เครื่องอื่น) —
สคริปต์นี้ backup ลง disk เดียวกันเท่านั้น ไม่ได้ป้องกันกรณี disk/เครื่องพังทั้งลูก

## Restore

```bash
./scripts/restore.sh ./backups/share_money_20260908_030000.dump
```

- `pg_restore --clean --if-exists` — ลบ object เดิมก่อน restore ทับ (schema `shmy` เดิมจะหายแล้วแทนที่ด้วยข้อมูลในไฟล์ backup)
- **ทดสอบ restore ใส่ database คนละตัวก่อนเสมอ** ถ้าไม่แน่ใจว่าไฟล์ backup ใช้ได้จริง อย่ารันตรงกับ DB
  ที่ใช้งานจริงทันที — เปลี่ยน `DB_NAME`/`POSTGRES_CONTAINER` ชี้ไป instance ทดสอบก่อน แล้วเช็คว่า
  แอปยิง query ผ่านปกติ (เช่นรัน `scripts/smoke-phase3.mjs`) ก่อนค่อย restore ทับของจริง

## Verify a backup is usable (ทำเป็นระยะ ไม่ใช่แค่ตอน incident)

1. สร้าง postgres container ทดสอบแยก (คนละ volume/port จากตัว prod)
2. `./scripts/restore.sh <dump-file>` ชี้ `POSTGRES_CONTAINER`/`DB_NAME` ไปตัวทดสอบ
3. รันแอป backend ชี้ไป DB ทดสอบนั้น แล้วเช็ค endpoint หลัก (`/actuator/health`, login, list debts)
4. ถ้าผ่าน แปลว่าไฟล์ backup ใช้ restore ได้จริง ไม่ใช่ไฟล์เสีย/ตัด

## Known gaps (ต้องทำตอน deploy environment จริง)

- ยังไม่มี off-site copy ของไฟล์ backup (ตอนนี้เก็บบน disk เดียวกับที่รัน backup)
- ยังไม่มี alert เมื่อ backup job ล้มเหลว (ตอนนี้ต้องเช็ค log/exit code เอง)
