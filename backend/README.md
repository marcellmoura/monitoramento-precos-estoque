# Backend

Base Spring Boot/Maven, pacote `br.com.marcellmoura.monitoramento`.

Cada funcionalidade futura terá seu próprio pacote, com entrada HTTP (`web`), regras de aplicação (`application`), domínio (`domain`) e acesso a dados (`persistence`) conforme necessário. Controllers delegam regras aos serviços. A extração de PDF não deve concentrar validação comercial ou cálculos.

Nesta etapa existe somente a inicialização da aplicação. Não há endpoints de negócio, autenticação, entidades ou conexão a banco. Testcontainers/PostgreSQL está disponível no escopo de testes; a integração real com banco pertence à TT-04.

Com Java 25 selecionado, executar nesta pasta:

```sh
./mvnw verify
./mvnw spring-boot:run
```

O Wrapper fixa Maven 3.10.0; não é necessário instalar Maven globalmente. Relatórios: `target/surefire-reports/`, `target/failsafe-reports/` e `target/site/jacoco/`.

`./mvnw test` executa testes rápidos (`*Test`/`*Tests`, também `Test*`/`*TestCase`); `./mvnw verify` inclui integração (`*IT`). O teste de contexto atual foi classificado como integração, via Failsafe. Ainda não há testes unitários Java de negócio; a rodada rápida pode ficar vazia nesta etapa. JaCoCo acumula as duas suítes no ciclo; executar `clean verify` para descartar dados antigos. Ver [CI](../docs/ci.md).

Servidor local: `127.0.0.1:8080`. `SERVER_PORT` e `SERVER_ADDRESS` permitem configurar o bind. Uma chamada a uma rota de negócio retorna 404 enquanto os incrementos funcionais não forem implementados.
