# Fatisda Here

Aplikasi Android absensi mahasiswa berbasis **Wi-Fi fingerprinting (KNN) + sensor gerak (PDR)**.
Absen hanya diterima jika HP mahasiswa terdeteksi berada di ruangan matkul.
Tiga ruangan MVP: **LAB 2 UPT**, **LAB UPT LT 3**, **B4.05**. SSID jaringan: `UNS SOLO`.

Dokumen perencanaan lengkap (acuan bersama tim): lihat `docs/` atau tautan yang dibagikan di grup.

## Struktur repo

fatisda-here/
├── app/src/main/java/com/fatisdahere/app/
│   ├── positioning/   KNN + aturan dua lapis (Kotlin murni, tanpa dependensi Android)
│   ├── sensing/       WifiScanner, sensor PDR (accelerometer + magnetometer)
│   ├── data/          model, repository, akses Firebase
│   ├── ui/            auth/, mahasiswa/, dosen/, admin/, debug/
│   └── di/            penyusunan dependensi manual (AppContainer, tanpa Hilt/Koin)
├── app/src/main/assets/           fingerprint_v1.json (berversi)
├── app/src/test/                  unit test positioning
├── tools/data/                    Excel mentah, notebook & skrip Python
└── docs/                          ERD, wireframe, diagram, laporan

Package aplikasi: `com.fatisdahere.app`

## Aturan kerja

- `main` hanya diubah lewat **pull request** yang sudah dicek satu anggota lain.
- Nama branch: `feature/<kode-layar-atau-modul>`, contoh: `feature/M-01-beranda`, `feature/positioning-engine`.
- Perubahan struktur data (bagian 9) atau kontrak modul (bagian 10) **ditulis di dokumen dulu**, baru kodenya.
- Saat memakai AI: tempelkan dokumen perencanaan sebagai konteks; kode hasil AI tetap dijalankan dan dicek sebelum PR.
- **`google-services.json` tidak di-commit** — dibagikan lewat grup.

## Minggu 1 (status)

- [ ] fingerprint_v1.json + heatmap RSSI — Andrew
- [ ] Wireframe layar Must — Rofiq
- [ ] ERD — Jauhar
- [x] Setup repo + proyek Firebase — Hesa
- [ ] Aplikasi testing KNN (scan, tebak ruangan, uji 3 ruangan + video) — Nopal
