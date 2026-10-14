# Hello BPMN – Spring Boot + Flowable

Tugas 2: instalasi dan konfigurasi tools BPMN pada lingkungan Java/Jakarta,
dibuktikan dengan proses *Hello World*.

- **Nama / NIM:** (isi)
- **Mata kuliah:** (isi)
- **Tools:** Java, Maven, Spring Boot 3.4.x, Flowable 7.2.0 (engine BPMN), H2 (database in-memory), bpmn.io (modeler)

## 1. Prasyarat dan instalasi

| Tools | Cara memasang | Cek |
|---|---|---|
| JDK 17 atau lebih baru (Eclipse Temurin) | adoptium.net | `java -version` |
| Maven 3.9+ | maven.apache.org, ekstrak, tambahkan folder `bin` ke `Path` | `mvn -v` |
| IDE | IntelliJ IDEA Community / VS Code | buka project ini |
| Modeler BPMN | demo.bpmn.io di browser (tanpa instalasi) | diagram tampil |

Flowable tidak diinstal manual. Library-nya diunduh otomatis oleh Maven dari `pom.xml`.

## 2. Struktur project

```
hello-bpmn/
├── pom.xml                                   # dependency Flowable + H2
├── src/main/java/id/ac/uinjkt/hellobpmn/
│   ├── HelloBpmnApplication.java             # titik masuk, memulai proses lalu berhenti
│   └── HelloDelegate.java                    # kode Java yang dipanggil service task
├── src/main/resources/
│   ├── application.properties                # konfigurasi database, Flowable, dan log
│   └── processes/hello.bpmn20.xml            # model BPMN
└── docs/screenshots/                         # taruh bukti screenshot di sini
```

## 3. Konfigurasi

1. `pom.xml` menambahkan `flowable-spring-boot-starter-process` dan `h2`.
2. `application.properties` mengatur database H2 in-memory, mematikan banner, dan membatasi log ke ERROR.
3. File BPMN diletakkan di `src/main/resources/processes/` sehingga otomatis di-*deploy* saat aplikasi start.

## 4. Cara menjalankan

Dari folder yang berisi `pom.xml`:

```bash
mvn -q spring-boot:run
```

Opsi `-q` menyembunyikan log Maven. Hasil yang diharapkan di console hanya:

```
Hello World
```

## 5. Cara kerja singkat

1. Aplikasi start, Flowable membuat tabel di H2 dan men-deploy `hello.bpmn20.xml`.
2. `CommandLineRunner` memanggil `startProcessInstanceByKey("helloProcess")`.
3. Engine menjalankan alur: **Mulai → Cetak Hello World → Selesai**.
4. Service task memanggil bean `helloDelegate` (`HelloDelegate.execute`) yang mencetak `Hello World`.
5. Aplikasi menutup dirinya sendiri setelah proses selesai.

## 6. Bukti pengujian

Simpan di `docs/screenshots/`:

1. `java -version` dan `mvn -v`
2. Diagram BPMN di demo.bpmn.io
3. Output console hello world
4. Rekaman layar proses menjalankan aplikasi

## 7. Kendala dan solusi

(Isi dengan kendala yang kamu alami sendiri.)
