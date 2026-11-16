package br.com.marcellmoura.monitoramento;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import br.com.marcellmoura.monitoramento.support.PostgreSqlTestConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@AutoConfigureMockMvc
@Import(PostgreSqlTestConfiguration.class)
class MonitoramentoApplicationIT {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exigeAutenticacaoParaRotasProtegidas() throws Exception {
		mockMvc.perform(get("/api/nao-configurado"))
				.andExpect(status().isUnauthorized());
	}

}
