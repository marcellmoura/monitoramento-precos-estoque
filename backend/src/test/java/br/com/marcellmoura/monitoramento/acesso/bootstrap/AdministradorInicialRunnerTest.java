package br.com.marcellmoura.monitoramento.acesso.bootstrap;

import java.io.Console;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import br.com.marcellmoura.monitoramento.acesso.application.AdministradorInicialService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdministradorInicialRunnerTest {
    @Test void recusaEntradaSemTerminalInterativo() {
        var service = mock(AdministradorInicialService.class);
        var runner = new AdministradorInicialRunner(service);
        assertThatThrownBy(() -> runner.executar(null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("terminal interativo");
        verifyNoInteractions(service);
    }
    @Test void criaContaSomenteAposConfirmacaoELimpaBuffersSemExibirSenha() {
        var service = mock(AdministradorInicialService.class);
        var console = terminal();
        char[] senha = "Temporaria-teste-123".toCharArray();
        char[] confirmacao = senha.clone();
        when(console.readPassword(anyString())).thenReturn(senha, confirmacao);
        doAnswer(invocation -> {
            assertThat((char[]) invocation.getArgument(2)).isEqualTo("Temporaria-teste-123".toCharArray());
            return null;
        }).when(service).criar(eq("Marina Teste"), eq("marina@example.com"), any(char[].class));
        new AdministradorInicialRunner(service).executar(console);
        verify(service).criar(eq("Marina Teste"), eq("marina@example.com"), same(senha));
        assertThat(senha).containsOnly('\0');
        assertThat(confirmacao).containsOnly('\0');
        verify(console).printf("Administrador criado. A troca de senha será obrigatória no primeiro acesso.%n");
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void recusaSenhaAusenteOuConfirmacaoDiferenteELimpaBuffers(int caso) {
        var service = mock(AdministradorInicialService.class);
        var console = terminal();
        char[] senha = caso == 0 ? null : "Temporaria-teste-123".toCharArray();
        char[] confirmacao = caso == 1 ? null : "Outra-senha-teste-456".toCharArray();
        when(console.readPassword(anyString())).thenReturn(senha, confirmacao);
        assertThatThrownBy(() -> new AdministradorInicialRunner(service).executar(console))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("As senhas não conferem.");
        verifyNoInteractions(service);
        if (senha != null) { assertThat(senha).containsOnly('\0'); }
        if (confirmacao != null) { assertThat(confirmacao).containsOnly('\0'); }
    }
    @Test void limpaBuffersMesmoQuandoBaseRecusaInicializacao() {
        var service = mock(AdministradorInicialService.class);
        var console = terminal();
        when(console.readLine(anyString())).thenReturn(null);
        char[] senha = "Temporaria-teste-123".toCharArray();
        char[] confirmacao = Arrays.copyOf(senha, senha.length);
        when(console.readPassword(anyString())).thenReturn(senha, confirmacao);
        doThrow(new IllegalStateException("Base inicializada")).when(service).criar(eq(""), eq(""), any(char[].class));
        assertThatThrownBy(() -> new AdministradorInicialRunner(service).executar(console))
                .isInstanceOf(IllegalStateException.class);
        assertThat(senha).containsOnly('\0');
        assertThat(confirmacao).containsOnly('\0');
    }
    private Console terminal() {
        var console = mock(Console.class);
        when(console.readLine(anyString())).thenReturn("Marina Teste", "marina@example.com");
        return console;
    }
}
