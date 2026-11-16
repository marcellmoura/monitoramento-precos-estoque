package br.com.marcellmoura.monitoramento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MonitoramentoApplication {

	public static void main(String[] args) {
		if (java.util.Arrays.asList(args).contains("--inicializar-admin")) {
            var application = new SpringApplication(MonitoramentoApplication.class);
            application.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
            application.setAdditionalProfiles("bootstrap-admin");
            try (var context = application.run(args)) {
                // O procedimento local termina sem iniciar servidor HTTP.
            }
        } else {
            SpringApplication.run(MonitoramentoApplication.class, args);
        }
	}

}
