package br.com.marcellmoura.monitoramento;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@AutoConfigureMockMvc
class MonitoramentoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void inicializaAplicacaoSemRotasDeNegocio() throws Exception {
		mockMvc.perform(get("/api/nao-configurado"))
				.andExpect(status().isNotFound());
	}

}
