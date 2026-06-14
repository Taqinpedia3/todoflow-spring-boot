# Ringkasan Belajar Todo API

Project ini adalah REST API Todo dengan Java Spring Boot. Fitur akhirnya:

- Dashboard web
- Register user
- Login user
- Password hashing dengan BCrypt
- JWT access token
- Todo CRUD
- Edit Todo lewat modal
- Reminder/alarm Todo
- Todo hanya bisa diakses oleh pemiliknya
- Pagination, sorting, dan filter
- PostgreSQL via Docker
- H2 untuk mode cepat lokal
- Flyway migration
- Automated test

## Alur Besar Aplikasi

1. User membuka dashboard di `/`.
2. User register lewat form atau `POST /api/auth/register`.
3. Password user di-hash dengan BCrypt sebelum disimpan.
4. User login lewat form atau `POST /api/auth/login`.
5. Server mengembalikan JWT access token.
6. Dashboard menyimpan token di `localStorage`.
7. Client mengirim token lewat header `Authorization: Bearer <token>`.
8. Spring Security memvalidasi token.
9. Endpoint Todo membaca email user dari token.
10. User bisa menambah `dueAt` sebagai waktu alarm.
11. Query Todo selalu difilter berdasarkan owner user tersebut.
12. Dashboard mengecek Todo yang waktunya sudah lewat dan memberi alarm.

## Konsep Yang Dipakai

### Controller

Controller menerima HTTP request dan mengembalikan response JSON. Contoh:

- `AuthController`
- `TodoController`

Controller sebaiknya tipis. Logic utama ditaruh di service.

### Static Frontend

Dashboard disimpan di `src/main/resources/static`. Spring Boot otomatis menyajikan file static di root URL.

File utamanya:

- `index.html` untuk struktur halaman
- `styles.css` untuk tampilan
- `app.js` untuk interaksi dan request API

Frontend memakai `fetch()` untuk memanggil backend. Setelah login berhasil, token JWT disimpan di browser dan dipakai lagi saat membuat, membaca, mengubah, atau menghapus Todo.

### Reminder/Alarm

Todo punya field `dueAt`, yaitu waktu kapan alarm harus aktif.

Alurnya:

1. User memilih waktu alarm di dashboard.
2. Frontend mengirim `dueAt` ke backend dalam format ISO timestamp.
3. Backend menyimpan `dueAt` ke kolom `todo.due_at`.
4. Dashboard mengecek Todo aktif secara berkala.
5. Jika `dueAt` sudah lewat dan Todo belum selesai, browser memberi alarm.

Alarm di project ini berjalan di sisi browser. Jadi alarm aktif saat dashboard sedang dibuka.

### Service

Service berisi business logic. Contoh:

- register user
- cek duplicate email
- hash password
- login
- generate token
- membuat Todo milik user tertentu

### Repository

Repository berhubungan dengan database lewat Spring Data JPA.

Contoh:

- `AppUserRepository`
- `TodoRepository`

### Entity

Entity mewakili tabel database.

Contoh:

- `AppUser`
- `Todo`

Entity tidak langsung dijadikan response API. Kita memakai DTO.

### DTO

DTO adalah bentuk data untuk request/response API.

Contoh request:

- `RegisterRequest`
- `LoginRequest`
- `TodoRequest`

Contoh response:

- `UserResponse`
- `LoginResponse`
- `TodoResponse`
- `PageResponse`
- `ApiErrorResponse`

### Validation

Validation mencegah data buruk masuk ke aplikasi.

Contoh:

- email harus valid
- password minimal 8 karakter
- title Todo wajib diisi

### Error Handling

`ApiExceptionHandler` membuat error response konsisten.

Contoh:

- `400 Bad Request` untuk validation error
- `401 Unauthorized` untuk login/token bermasalah
- `404 Not Found` untuk Todo yang tidak ada atau bukan milik user
- `409 Conflict` untuk email duplicate

### Security

Security memakai Spring Security.

Yang dibuka tanpa token:

- `POST /api/auth/register`
- `POST /api/auth/login`

Yang wajib token:

- semua endpoint `/api/todos`

### JWT

JWT adalah token yang berisi identitas user. Di project ini subject JWT adalah email user. Backend memakai email itu untuk mencari Todo milik user tersebut.

### Database

Mode `dev` memakai H2 in-memory agar cepat.

Mode `postgres` memakai PostgreSQL lewat Docker agar lebih mirip production.

### Flyway

Flyway membuat struktur tabel dari file migration:

```text
src/main/resources/db/migration/V1__create_users_and_todos.sql
```

Ini lebih profesional daripada mengandalkan Hibernate membuat tabel otomatis.

### Pagination, Sorting, Filter

Endpoint `GET /api/todos` mendukung:

- `page`
- `size`
- `sortBy`
- `direction`
- `completed`
- `q`

Contoh:

```text
/api/todos?completed=false&q=belajar&page=0&size=5&sortBy=title&direction=asc
```

## Cara Latihan

1. Jalankan aplikasi.
2. Register user baru.
3. Login untuk mendapatkan token.
4. Simpan token ke variabel PowerShell.
5. Buat beberapa Todo.
6. Coba filter dan pagination.
7. Register user kedua.
8. Login user kedua.
9. Pastikan user kedua tidak melihat Todo user pertama.

## Hal Yang Perlu Kamu Kuasai Dari Project Ini

- Bedanya controller, service, repository, entity, DTO
- Cara request JSON masuk ke Spring Boot
- Cara validasi request
- Cara response error dibuat konsisten
- Cara JPA menyimpan data
- Cara relasi `Todo -> AppUser`
- Cara password hashing bekerja
- Cara login memverifikasi password
- Cara JWT melindungi endpoint
- Cara frontend memanggil REST API dengan `fetch()`
- Cara menyimpan token di browser
- Cara membuat modal edit di frontend
- Cara membuat reminder sederhana dengan `dueAt`
- Cara memakai Notification API dan Web Audio API
- Cara database migration bekerja
- Cara menulis test untuk API

## Next Upgrade Setelah Ini

- Refresh token
- Logout/token blacklist
- Role user/admin
- Swagger/OpenAPI documentation
- CI/CD dengan GitHub Actions
- Deploy ke Railway/Render/VPS
- Observability: logging, metrics, health check
- Rate limiting
