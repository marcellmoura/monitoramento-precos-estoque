# Backend

Base Spring Boot/Maven, pacote `br.com.marcellmoura.monitoramento`.

Cada funcionalidade futura terá seu próprio pacote, com entrada HTTP (`http`), regras de aplicação (`application`), domínio (`domain`) e acesso a dados (`persistence`) conforme necessário. Controllers delegam regras aos serviços. A extração de PDF não deve concentrar validação comercial ou cálculos.

Persistência inicial PostgreSQL/JPA/Flyway configurada na TT-04, com entidade/repositório de usuários. Fluxo de autenticação/primeiro acesso em [acesso](../docs/acesso.md); módulos de negócio pendentes. Banco, variáveis e execução em [persistência](../docs/persistencia.md). O backend exige conexão PostgreSQL; os testes usam containers próprios, sem depender do banco local.

Com Java 25 selecionado, executar nesta pasta:

```sh
./mvnw verify
./mvnw spring-boot:run
```

O Wrapper fixa Maven 3.10.0; não é necessário instalar Maven globalmente. Relatórios: `target/surefire-reports/`, `target/failsafe-reports/` e `target/site/jacoco/`.

`./mvnw test` executa testes rápidos (`*Test`/`*Tests`, também `Test*`/`*TestCase`); `./mvnw verify` inclui integração (`*IT`). Contexto, persistência de usuários e migrations são testes de integração via Failsafe. Testes rápidos verificam a entrada interativa e a limpeza de buffers do bootstrap; a integração também verifica a recusa da execução sem terminal. JaCoCo acumula as duas suítes no ciclo; executar `clean verify` para descartar dados antigos. Ver [CI](../docs/ci.md).

Servidor local: `127.0.0.1:8080`. `SERVER_PORT` e `SERVER_ADDRESS` permitem configurar o bind. Rotas protegidas exigem sessão (401); senha temporária restringe funções (403); rota inexistente para usuário com senha definitiva retorna 404.
