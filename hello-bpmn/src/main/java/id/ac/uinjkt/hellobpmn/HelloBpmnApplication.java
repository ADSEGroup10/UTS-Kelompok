package id.ac.uinjkt.hellobpmn;

import org.flowable.engine.RuntimeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HelloBpmnApplication {

    public static void main(String[] args) {
        // Tutup aplikasi otomatis setelah proses selesai, supaya program langsung berhenti
        System.exit(SpringApplication.exit(SpringApplication.run(HelloBpmnApplication.class, args)));
    }

    /** Menjalankan proses "helloProcess" sekali saat aplikasi start. */
    @Bean
    CommandLineRunner jalankanProses(RuntimeService runtimeService) {
        return args -> runtimeService.startProcessInstanceByKey("helloProcess");
    }
}
