package id.ac.uinjkt.cutibpmn;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/** Service task "Validasi Sisa Cuti": menghasilkan variabel proses "valid". */
@Component("validasiCutiDelegate")
public class ValidasiCutiDelegate implements JavaDelegate {

    private final CutiService cutiService;

    public ValidasiCutiDelegate(CutiService cutiService) {
        this.cutiService = cutiService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String nama = (String) execution.getVariable("nama");
        int jumlahHari = ((Number) execution.getVariable("jumlahHari")).intValue();
        int sisa = cutiService.getSisa(nama);

        boolean valid = jumlahHari > 0 && jumlahHari <= sisa;
        execution.setVariable("valid", valid);

        System.out.println(">>> [VALIDASI] " + nama + " minta " + jumlahHari
                + " hari, sisa cuti " + sisa + " -> " + (valid ? "VALID" : "TIDAK VALID"));
    }
}
