package id.ac.uinjkt.cutibpmn;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** REST API tipis yang menjadi "form" bagi user task dan penghubung ke engine BPMN. */
@RestController
@RequestMapping("/api")
public class CutiController {

    private static final String PROCESS_KEY = "pengajuanCuti";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final CutiService cutiService;

    public CutiController(RuntimeService runtimeService, TaskService taskService,
                          HistoryService historyService, CutiService cutiService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.cutiService = cutiService;
    }

    /** Pemohon mengajukan cuti -> memulai proses BPMN. */
    @PostMapping("/cuti")
    public Map<String, Object> ajukan(@RequestBody Map<String, Object> body, Authentication authentication) {
        Object hariRaw = body.get("jumlahHari");
        if (hariRaw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jumlahHari wajib diisi");
        }
        int jumlahHari;
        try {
            jumlahHari = Integer.parseInt(String.valueOf(hariRaw));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jumlahHari harus berupa angka", e);
        }
        if (jumlahHari < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jumlahHari minimal 1");
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("nama", authentication.getName());
        vars.put("jumlahHari", jumlahHari);
        vars.put("alasan", body.getOrDefault("alasan", ""));

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(PROCESS_KEY, vars);
        return status(pi.getId());
    }

    /** Cek status pengajuan. */
    @GetMapping("/cuti/{id}")
    public Map<String, Object> cekStatus(@PathVariable String id, Authentication authentication) {
        Map<String, Object> result = status(id);
        if (!authentication.getName().equalsIgnoreCase(String.valueOf(result.get("pemohon")))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pengajuan hanya dapat dilihat oleh pemiliknya");
        }
        return result;
    }

    /** Daftar pengajuan milik karyawan yang sedang login. */
    @GetMapping("/cuti")
    public List<Map<String, Object>> daftarPengajuan(Authentication authentication) {
        return historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .variableValueEquals("nama", authentication.getName())
                .list().stream()
                .map(instance -> status(instance.getId()))
                .toList();
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()).toLowerCase())
                .findFirst()
                .orElse("");
        return Map.of("username", authentication.getName(), "role", role);
    }

    /** Daftar user task "Persetujuan Atasan" yang menunggu. */
    @GetMapping("/tugas")
    public List<Map<String, Object>> daftarTugas() {
        return taskService.createTaskQuery()
                .taskCandidateGroup("atasan")
                .orderByTaskCreateTime().asc()
                .list().stream().map(t -> {
                    Map<String, Object> v = taskService.getVariables(t.getId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("tugas", t.getName());
                    m.put("processInstanceId", t.getProcessInstanceId());
                    m.put("pemohon", v.get("nama"));
                    m.put("jumlahHari", v.get("jumlahHari"));
                    m.put("alasan", v.get("alasan"));
                    m.put("sisaCuti", cutiService.getSisa(String.valueOf(v.get("nama"))));
                    return m;
                }).toList();
    }

    /** Atasan menyelesaikan user task: disetujui=true/false -> menentukan jalur gateway. */
    @PostMapping("/tugas/{id}/selesai")
    public Map<String, Object> selesaikan(@PathVariable String id, @RequestParam boolean disetujui) {
        Task task = taskService.createTaskQuery().taskId(id).singleResult();
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tugas tidak ditemukan");
        }
        String processInstanceId = task.getProcessInstanceId();
        taskService.complete(id, Map.of("disetujui", disetujui));
        return status(processInstanceId);
    }

    private Map<String, Object> status(String id) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("processInstanceId", id);

        boolean aktif = runtimeService.createProcessInstanceQuery().processInstanceId(id).count() > 0;
        Object pemohon = historicVar(id, "nama");
        r.put("pemohon", pemohon);
        if (aktif) {
            r.put("status", "MENUNGGU_PERSETUJUAN_ATASAN");
        } else {
            r.put("status", historicVar(id, "hasil"));
            r.put("catatan", historicVar(id, "catatan"));
        }
        if (pemohon != null) {
            r.put("sisaCuti", cutiService.getSisa(String.valueOf(pemohon)));
        }
        return r;
    }

    private Object historicVar(String processInstanceId, String name) {
        HistoricVariableInstance v = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).variableName(name).singleResult();
        return v == null ? null : v.getValue();
    }
}
