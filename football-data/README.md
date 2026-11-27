# ZORA FOOTBALL DATA

Backend Node.js untuk mengumpulkan data pertandingan dan klasemen ke satu API ZORA.

## Kompetisi awal
- Premier League — ESPN `eng.1`, football-data.org `PL`
- La Liga — ESPN `esp.1`, football-data.org `PD`
- Serie A — ESPN `ita.1`, football-data.org `SA`
- Bundesliga — ESPN `ger.1`, football-data.org `BL`
- Ligue 1 — ESPN `fra.1`, football-data.org `FL`
- Liga 1 Indonesia — API-Sports (butuh `APISPORTS_KEY`)
- UEFA Champions League — ESPN `uefa.champions`, football-data.org `CL`

## Menjalankan lokal
1. Gunakan Node.js 20 atau lebih baru.
2. Salin `.env.example` menjadi `.env`.
3. Isi token provider yang tersedia. Jangan pernah menaruh token di APK atau mengunggah file `.env`.
4. Jalankan `npm install`, lalu `npm start`.

## Endpoint
- `GET /api/v1/health`
- `GET /api/v1/leagues`
- `GET /api/v1/matches/live`
- `GET /api/v1/matches?league=premier-league`
- `GET /api/v1/standings/premier-league`
- `POST /api/v1/admin/sync` dengan header `x-zora-admin-token`.

## Provider & batasan
ESPN digunakan sebagai sumber utama untuk enam kompetisi non-Indonesia. ESPN API bersifat tidak resmi dan dapat berubah. football-data.org menjadi fallback untuk kompetisi yang dicakup, tetapi memerlukan token dan kuota sesuai paket. Liga 1 Indonesia diarahkan ke API-Sports dan memerlukan key serta paket yang mendukung kompetisi/musim terkait. Pastikan ID liga dan musim dikonfirmasi pada akun provider sebelum produksi.

Backend menggunakan cache in-memory. Pada deployment multi-instance atau server yang sering restart, pindahkan cache ke Redis/Firestore/Postgres sebelum mengandalkan data ini untuk produksi. Endpoint sinkronisasi manual dilindungi token; untuk produksi tambahkan autentikasi admin, rate limit, logging, dan persistent storage.

## Integrasi Android
Aplikasi Android nantinya diarahkan ke base URL backend ZORA, bukan langsung ke ESPN/API-Sports. Simpan base URL sebagai konfigurasi aplikasi; semua API key provider tetap hanya di server.
