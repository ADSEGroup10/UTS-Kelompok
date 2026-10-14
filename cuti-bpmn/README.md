# Use Case: Pengajuan Cuti dengan BPMN, Spring Boot, dan Flowable

Tugas 3: membuat model proses bisnis dengan BPMN, lalu menjalankannya sebagai aplikasi Java.

- **Nama / NIM:** (isi)
- **Mata kuliah:** (isi)
- **Tools:** Java, Maven, Spring Boot 3.4.x, Flowable 7.2.0, H2 (file), bpmn.io (modeler)

## 1. Deskripsi use case

Seorang **karyawan** mengajukan cuti. Sistem memeriksa apakah sisa cuti cukup. Jika cukup,
**atasan** memutuskan menyetujui atau menolak. Hasil akhirnya (disetujui atau ditolak)
dikirim sebagai notifikasi, dan sisa cuti berkurang bila disetujui.

Jatah cuti awal setiap pemohon: 12 hari.

## 2. Model BPMN

File: `src/main/resources/processes/pengajuan-cuti.bpmn20.xml`
(dapat dibuka di demo.bpmn.io dengan menyeret file ke halaman tersebut).

File BPMN sudah menyertakan **BPMN DI (Diagram Interchange)** lengkap: posisi start/end event,
service task, user task, gateway, serta waypoint untuk seluruh sequence flow. Jadi diagram dapat
langsung dirender oleh bpmn.io/Camunda Modeler/Flowable Design tanpa menggambar ulang.

### Melihat atau mengedit diagram

1. Buka [demo.bpmn.io](https://demo.bpmn.io/).
2. Seret file `src/main/resources/processes/pengajuan-cuti.bpmn20.xml` ke canvas, atau pilih **Open**.
3. Diagram akan tampil dengan alur utama kiri-ke-kanan, jalur penolakan otomatis di bawah gateway pertama,
   dan jalur penolakan atasan di bawah gateway keputusan.
4. Untuk menyimpan hasil visual, gunakan **File > Export as SVG** atau **File > Save BPMN 2.0 XML**.

Alternatif lokal: buka file yang sama menggunakan Camunda Modeler atau Flowable Design. Jangan menghapus
bagian `<bpmndi:BPMNDiagram>` saat menyimpan, karena bagian tersebut adalah layout visual diagram.

```
Pengajuan Diterima -> Validasi Sisa Cuti -> <Sisa cuti cukup?>
    ya    -> Persetujuan Atasan -> <Disetujui?>
                  ya    -> Kurangi Sisa Cuti & Notifikasi Disetujui -> Selesai
                  tidak -> Notifikasi Ditolak -> Selesai
    tidak -> Tolak Otomatis -> Selesai
```

| Elemen BPMN | Nama di model | Fungsi |
|---|---|---|
| Start event | Pengajuan Diterima | Awal proses, dipicu saat pemohon mengajukan |
| Service task | Validasi Sisa Cuti | Memanggil `ValidasiCutiDelegate` (Java) untuk membandingkan jumlah hari dengan sisa cuti |
| Exclusive gateway | Sisa cuti cukup? | Memilih jalur berdasarkan variabel `valid` |
| User task | Persetujuan Atasan | Menunggu keputusan manusia (grup `atasan`) |
| Exclusive gateway | Disetujui? | Memilih jalur berdasarkan variabel `disetujui` |
| Service task | Kurangi Sisa Cuti & Notifikasi Disetujui | Mengurangi sisa cuti, mencatat hasil `DISETUJUI` |
| Service task | Notifikasi Ditolak | Mencatat hasil `DITOLAK` oleh atasan |
| Service task | Tolak Otomatis | Mencatat hasil `DITOLAK` karena sisa cuti tidak cukup |
| End event | Selesai | Akhir proses |

### Variabel proses

| Variabel | Diisi oleh | Keterangan |
|---|---|---|
| `nama` | Identitas session login karyawan | Pemilik pengajuan, tidak dapat diisi oleh client |
| `jumlahHari`, `alasan` | Form karyawan (REST `POST /api/cuti`) | Data pengajuan |
| `valid` | `ValidasiCutiDelegate` | `true` bila jumlah hari <= sisa cuti |
| `disetujui` | Atasan (menyelesaikan user task) | Keputusan atasan |
| `hasil`, `catatan` | Delegate notifikasi | Hasil akhir proses |

## 3. Arsitektur dan alur kerja

```
Browser (index.html)  ->  CutiController (REST)  ->  Flowable engine  ->  file BPMN
                                                         |
                                                         +-> Delegate (Java) -> CutiService
```

1. Karyawan membuka `index.html` dan login melalui form aplikasi. Spring Security membuat session dan cookie `JSESSIONID`; `POST /api/cuti` mengambil nama dari identitas login, lalu memanggil `startProcessInstanceByKey("pengajuanCuti", variabel)`.
2. Engine menjalankan *service task* validasi (kode Java) dan mengisi `valid`.
3. *Gateway* membaca `valid`. Bila `false`, proses langsung ditolak otomatis dan selesai.
4. Bila `true`, proses berhenti di *user task* dan tersimpan di database engine.
5. Atasan melihat daftar tugas (`GET /api/tugas`) lalu menekan Setujui atau Tolak
   (`POST /api/tugas/{id}/selesai?disetujui=...`), yang memanggil `taskService.complete(...)`.
6. *Gateway* kedua membaca `disetujui`, lalu engine menjalankan service task notifikasi yang sesuai.

## 4.1 Autentikasi dan otorisasi

Aplikasi memakai session-based form login Spring Security dan dua user in-memory untuk demo:

| User | Password | Role | Akses |
|---|---|---|---|
| `budi` | `budi123` | `KARYAWAN` | Mengajukan dan melihat pengajuan milik sendiri |
| `atasan` | `atasan123` | `ATASAN` | Melihat dan menyelesaikan tugas persetujuan |

Password dapat diganti melalui `app.security.*` di `application.properties` atau environment-specific configuration.
Server menolak akses lintas role (karyawan ke `/api/tugas`, atasan ke `/api/cuti`) dan karyawan tidak dapat
mengganti pemilik pengajuan dengan mengirim nama lain.

Tombol **Logout** memakai native POST form ke endpoint bawaan Spring Security `/logout`. Endpoint ini menginvalidasi session,
menghapus autentikasi, dan menghapus cookie `JSESSIONID`, sehingga akun benar-benar keluar dari aplikasi.
Setelah itu halaman kembali ke form login; tidak ada cache HTTP Basic yang perlu dibersihkan.

## 5. Struktur project

```
cuti-bpmn/
├── pom.xml
├── src/main/java/id/ac/uinjkt/cutibpmn/
│   ├── CutiBpmnApplication.java          # titik masuk Spring Boot
│   ├── CutiController.java               # REST API (penghubung form dan engine)
│   ├── CutiService.java                  # penyimpan sisa cuti (in-memory)
│   ├── ValidasiCutiDelegate.java         # service task: validasi
│   ├── TolakOtomatisDelegate.java        # service task: tolak otomatis
│   ├── NotifikasiDisetujuiDelegate.java  # service task: setujui
│   └── NotifikasiDitolakDelegate.java    # service task: tolak oleh atasan
├── src/main/resources/
│   ├── application.properties
│   ├── processes/pengajuan-cuti.bpmn20.xml   # model BPMN
│   └── static/index.html                     # form sederhana
└── docs/screenshots/                         # bukti screenshot
```

## 6. Cara menjalankan

```bash
mvn spring-boot:run
```

Buka `http://localhost:8080` di browser. Hentikan dengan `Ctrl + C`.
Data disimpan di H2 in-memory, sehingga hilang saat aplikasi dimatikan.

## 7. Endpoint ringkas

| Method | Endpoint | Role | Fungsi |
|---|---|---|---|
| GET | `/api/me` | login | Menampilkan user dan role aktif |
| POST | `/logout` | login | Logout Spring Security: invalidate session dan hapus cookie `JSESSIONID` |
| POST | `/api/cuti` | KARYAWAN | Mengajukan cuti atas nama user login |
| GET | `/api/cuti` | KARYAWAN | Melihat riwayat pengajuan user login |
| GET | `/api/cuti/{id}` | KARYAWAN | Melihat satu pengajuan milik user login |
| GET | `/api/tugas` | ATASAN | Melihat user task BPMN yang menunggu |
| POST | `/api/tugas/{id}/selesai?disetujui=...` | ATASAN | Menyetujui atau menolak tugas |

## 8. Skenario pengujian

| # | Langkah | Hasil yang diharapkan |
|---|---|---|
| 1 | Budi, 3 hari, lalu atasan **Setujui** | `DISETUJUI`, sisa cuti Budi menjadi 9 |
| 2 | Budi, 20 hari | Langsung `DITOLAK` (tolak otomatis), atasan tidak menerima tugas |
| 3 | Ani, 2 hari, lalu atasan **Tolak** | `DITOLAK` oleh atasan, sisa cuti Ani tetap 12 |
| 4 | Budi, 10 hari (setelah skenario 1, sisa 9) | `DITOLAK` otomatis karena 10 > 9 |

Console aplikasi mencetak baris `>>> [VALIDASI]` dan `>>> [NOTIFIKASI]` untuk setiap langkah.

Alternatif tanpa browser (PowerShell):

```powershell
$karyawan = Get-Credential # gunakan budi / budi123
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/cuti -Credential $karyawan -ContentType "application/json" `
  -Body '{"jumlahHari":3,"alasan":"Acara keluarga"}'
$atasan = Get-Credential # gunakan atasan / atasan123
Invoke-RestMethod http://localhost:8080/api/tugas -Credential $atasan

```

## 9. Bukti (simpan di `docs/screenshots/`)

1. Diagram BPMN di demo.bpmn.io
2. Halaman form, sebelum dan sesudah mengajukan
3. Daftar tugas atasan dan hasil keputusan
4. Output console untuk skenario 1 sampai 4
5. Rekaman layar satu skenario lengkap

## 10. Kendala dan solusi

(Isi dengan kendala yang kamu alami sendiri.)
