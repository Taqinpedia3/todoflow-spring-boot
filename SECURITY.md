# Security Policy

## Reporting

Jangan membuka public issue untuk kerentanan yang dapat mengekspos data atau
kredensial. Laporkan secara privat kepada pemilik repository.

## Configuration

- Jangan commit `.env`, JWT secret, atau kredensial database.
- Gunakan secret acak minimal 32 karakter untuk `JWT_SECRET`.
- Ganti seluruh default credential sebelum deployment.
- Jalankan aplikasi production dengan PostgreSQL dan HTTPS.
