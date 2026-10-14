package id.ac.uinjkt.cutibpmn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/** Menyimpan sisa cuti per pemohon (in-memory, cukup untuk demo). */
@Service
public class CutiService {

    public static final int JATAH_CUTI = 12;

    private final Map<String, Integer> sisa = new ConcurrentHashMap<>();

    public int getSisa(String nama) {
        return sisa.getOrDefault(key(nama), JATAH_CUTI);
    }

    public void kurangi(String nama, int hari) {
        sisa.compute(key(nama), (k, v) -> (v == null ? JATAH_CUTI : v) - hari);
    }

    private String key(String nama) {
        return nama.trim().toLowerCase();
    }
}
