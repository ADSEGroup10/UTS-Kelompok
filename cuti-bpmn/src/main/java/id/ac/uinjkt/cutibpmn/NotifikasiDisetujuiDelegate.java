package id.ac.uinjkt.cutibpmn;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/** Service task "Kurangi Sisa Cuti & Notifikasi": dijalankan jika atasan menyetujui. */
@Component("notifikasiDisetujuiDelegate")
public class NotifikasiDisetujuiDelegate implements JavaDelegate {

    private final CutiService cutiService;

    public NotifikasiDisetujuiDelegate(CutiService cutiService) {
        this.cutiService = cutiService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String nama = (String) execution.getVariable("nama");
        int jumlahHari = ((Number) execution.getVariable("jumlahHari")).intValue();

        cutiService.kurangi(nama, jumlahHari);
        execution.setVariable("hasil", "DISETUJUI");
        execution.setVariable("catatan", "Disetujui oleh atasan");
        System.out.println(">>> [NOTIFIKASI] Pengajuan cuti " + nama + " DISETUJUI. Sisa cuti sekarang "
                + cutiService.getSisa(nama) + " hari");
    }
}
