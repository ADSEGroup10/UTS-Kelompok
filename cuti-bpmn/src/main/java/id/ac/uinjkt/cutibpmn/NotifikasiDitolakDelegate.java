package id.ac.uinjkt.cutibpmn;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/** Service task "Notifikasi Ditolak": dijalankan jika atasan menolak. */
@Component("notifikasiDitolakDelegate")
public class NotifikasiDitolakDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String nama = (String) execution.getVariable("nama");
        execution.setVariable("hasil", "DITOLAK");
        execution.setVariable("catatan", "Ditolak oleh atasan");
        System.out.println(">>> [NOTIFIKASI] Pengajuan cuti " + nama + " DITOLAK oleh atasan");
    }
}
