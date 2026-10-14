package id.ac.uinjkt.hellobpmn;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/** Dipanggil oleh service task "Cetak Hello World" pada model BPMN. */
@Component("helloDelegate")
public class HelloDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("Hello World");
    }
}
