package id.ac.uinjkt.cutibpmn;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/** Service task "Tolak Otomatis": dijalankan jika sisa cuti tidak cukup. */
@Component("tolakOtomatisDelegate")
public class TolakOtomatisDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String nama = (String) execution.getVariable("nama");
        execution.setVariable("hasil", "DITOLAK");
        execution.setVariable("catatan", "Ditolak otomatis: sisa cuti tidak cukup atau jumlah hari tidak valid");
        System.out.println(">>> [NOTIFIKASI] Pengajuan cuti " + nama + " DITOLAK OTOMATIS (sisa cuti tidak cukup)");
    }
}
