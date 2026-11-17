package br.com.sicomat.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sicomat.config.UsuarioAutenticado;
import br.com.sicomat.equipe.infrastructure.persistence.EquipeJpaEntity;
import br.com.sicomat.equipe.infrastructure.persistence.EquipeJpaRepository;
import br.com.sicomat.equipe.infrastructure.persistence.RecebedorJpaEntity;
import br.com.sicomat.equipe.infrastructure.persistence.RecebedorJpaRepository;
import br.com.sicomat.estoque.StatusItem;
import br.com.sicomat.estoque.infrastructure.persistence.ItemMaterialJpaEntity;
import br.com.sicomat.estoque.infrastructure.persistence.ItemMaterialJpaRepository;
import br.com.sicomat.estoque.infrastructure.persistence.MaterialJpaEntity;
import br.com.sicomat.estoque.infrastructure.persistence.MaterialJpaRepository;
import br.com.sicomat.movimentacao.StatusEmpenho;
import br.com.sicomat.seguranca.CodificadorSenha;
import br.com.sicomat.usuario.PerfilUsuario;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaEntity;
import br.com.sicomat.usuario.infrastructure.persistence.UsuarioJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiSecurityIntegrationTest {

    private static final String SETOR = "UNIDADE-ALFA";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioJpaRepository usuarios;
    @Autowired private MaterialJpaRepository materiais;
    @Autowired private ItemMaterialJpaRepository itens;
    @Autowired private EquipeJpaRepository equipes;
    @Autowired private RecebedorJpaRepository recebedores;

    @Test
    void protegeRotasPrivadasEDevolve401SemDesafioBasic() throws Exception {
        mvc.perform(get("/api/v1/materiais"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void disponibilizaIdentidadeSemDadosDeSenha() throws Exception {
        mvc.perform(get("/api/v1/conta").with(autenticacao(42L, PerfilUsuario.OPERADOR_ESTOQUE, SETOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("OPERADOR_ESTOQUE"))
                .andExpect(jsonPath("$.setor").value(SETOR))
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.pinHash").doesNotExist());
    }

    @Test
    void entregaTokenCsrfSemPermitirCacheDaResposta() throws Exception {
        mvc.perform(get("/api/v1/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void exigeCsrfEImpedeOperadorDeCadastrarMaterial() throws Exception {
        mvc.perform(post("/api/v1/materiais")
                        .with(autenticacao(42L, PerfilUsuario.ADMINISTRADOR_ESTOQUE, SETOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"HT-01\",\"nome\":\"Rádio HT\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/materiais")
                        .with(autenticacao(42L, PerfilUsuario.OPERADOR_ESTOQUE, SETOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"HT-01\",\"nome\":\"Rádio HT\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/materiais")
                        .with(autenticacao(42L, PerfilUsuario.ADMINISTRADOR_ESTOQUE, SETOR))
                        .with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"HT-02\",\"nome\":\"Rádio HT\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aplicaAutorizacaoPorPerfilNasRotasAdministrativas() throws Exception {
        mvc.perform(get("/api/v1/usuarios")
                        .with(autenticacao(42L, PerfilUsuario.ADMINISTRADOR_ESTOQUE, SETOR)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/usuarios")
                        .with(autenticacao(42L, PerfilUsuario.OPERADOR_ESTOQUE, SETOR)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/equipes")
                        .with(autenticacao(42L, PerfilUsuario.OPERADOR_ESTOQUE, SETOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Equipe proibida\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void limitaTamanhoDaPaginaParaConterConsumoDeRecursos() throws Exception {
        mvc.perform(get("/api/v1/empenhos/abertos?tamanho=101")
                        .with(autenticacao(42L, PerfilUsuario.OPERADOR_ESTOQUE, SETOR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ignoraSetorEnviadoPeloClienteEUsaSetorAutenticado() throws Exception {
        MvcResult materialResult = mvc.perform(post("/api/v1/materiais")
                        .with(autenticacao(42L, PerfilUsuario.ADMINISTRADOR_ESTOQUE, SETOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"HT-SCOPE\",\"nome\":\"Rádio HT\",\"setor\":\"UNIDADE-BETA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setor").doesNotExist())
                .andReturn();
        long id = objectMapper.readTree(materialResult.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/v1/materiais")
                        .with(autenticacao(42L, PerfilUsuario.ADMINISTRADOR_ESTOQUE, "UNIDADE-BETA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(put("/api/v1/materiais/{id}", id)
                        .with(autenticacao(42L, PerfilUsuario.ADMINISTRADOR_ESTOQUE, SETOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"HT-SCOPE\",\"nome\":\"Alterado\",\"setor\":\"UNIDADE-BETA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Alterado"));
    }

    @Test
    void corsAceitaSomenteOrigemConfigurada() throws Exception {
        mvc.perform(options("/api/v1/materiais")
                        .header("Origin", "https://frontend.sicomat.test")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://frontend.sicomat.test"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mvc.perform(options("/api/v1/materiais")
                        .header("Origin", "https://origem-nao-autorizada.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void naoPermiteLerOuEditarMaterialDeOutroSetor() throws Exception {
        MaterialJpaEntity material = materiais.save(new MaterialJpaEntity("UNIDADE-BETA", "HT-02", "Rádio HT"));

        mvc.perform(get("/api/v1/materiais")
                        .with(autenticacao(42L, PerfilUsuario.ADMINISTRADOR_ESTOQUE, SETOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());

        mvc.perform(put("/api/v1/materiais/{id}", material.getId())
                        .with(autenticacao(42L, PerfilUsuario.ADMINISTRADOR_ESTOQUE, SETOR))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"HT-02\",\"nome\":\"Alterado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void administraCadastrosDoEstoquePelasRotasDaApi() throws Exception {
        RequestPostProcessor administrador = autenticacao(42L, PerfilUsuario.ADMINISTRADOR_ESTOQUE, SETOR);

        MvcResult equipeResult = mvc.perform(post("/api/v1/equipes")
                        .with(administrador).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Equipe Api\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Equipe Api"))
                .andReturn();
        long equipeId = objectMapper.readTree(equipeResult.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/v1/equipes").with(administrador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(equipeId));

        MvcResult recebedorResult = mvc.perform(post("/api/v1/recebedores")
                        .with(administrador).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Recebedor API\",\"matricula\":\"RC-API\",\"pinInicial\":\"548219\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pinHash").doesNotExist())
                .andReturn();
        long recebedorId = objectMapper.readTree(recebedorResult.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/v1/recebedores/{id}/pin", recebedorId)
                        .with(administrador).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoPin\":\"812947\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/recebedores").with(administrador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pinHash").doesNotExist());

        MvcResult materialResult = mvc.perform(post("/api/v1/materiais")
                        .with(administrador).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"HT-API\",\"nome\":\"Rádio HT\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long materialId = objectMapper.readTree(materialResult.getResponse().getContentAsString()).get("id").asLong();
        MvcResult itemResult = mvc.perform(post("/api/v1/materiais/{id}/itens", materialId)
                        .with(administrador).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroPatrimonio\":\"PAT-API\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long itemId = objectMapper.readTree(itemResult.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/v1/itens/{id}/inativacao", itemId)
                        .with(administrador).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/itens/disponiveis").with(administrador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void percorreEmpenhoConfirmacaoDevolucaoEConsultaDePendencias() throws Exception {
        UsuarioJpaEntity operador = usuarios.save(new UsuarioJpaEntity(
                "Operador Teste", "OP-TESTE", CodificadorSenha.codificar("SenhaOperadorTeste123!"),
                PerfilUsuario.OPERADOR_ESTOQUE, SETOR, "DIA"));
        MaterialJpaEntity material = materiais.save(new MaterialJpaEntity(SETOR, "HT-10", "Rádio HT"));
        ItemMaterialJpaEntity itemMaterial = itens.save(
                new ItemMaterialJpaEntity(material, "PAT-HT-10", StatusItem.DISPONIVEL));
        EquipeJpaEntity equipe = equipes.save(new EquipeJpaEntity(SETOR, "Equipe Alfa"));
        RecebedorJpaEntity recebedor = recebedores.save(new RecebedorJpaEntity(
                SETOR, "Recebedor Teste", "RC-TESTE", CodificadorSenha.codificar("123456")));
        RequestPostProcessor operadorAuth = autenticacao(operador.getId(), PerfilUsuario.OPERADOR_ESTOQUE, SETOR);

        MvcResult criacao = mvc.perform(post("/api/v1/empenhos")
                        .with(operadorAuth)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recebedorId":%d,"equipeId":%d,"itemMaterialIds":[%d],"observacao":"Início do serviço"}
                                """.formatted(recebedor.getId(), equipe.getId(), itemMaterial.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(StatusEmpenho.PENDENTE.name()))
                .andReturn();
        JsonNode empenhoCriado = objectMapper.readTree(criacao.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long empenhoId = empenhoCriado.get("id").asLong();
        long itemEmpenhoId = empenhoCriado.get("itens").get(0).get("id").asLong();

        mvc.perform(post("/api/v1/empenhos/{id}/confirmacao", empenhoId)
                        .with(operadorAuth)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"123456\",\"justificativa\":\"Recebedor conferido\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(true))
                .andExpect(jsonPath("$.empenho.status").value(StatusEmpenho.ABERTO.name()));

        mvc.perform(post("/api/v1/empenhos/{id}/devolucoes", empenhoId)
                        .with(operadorAuth)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"devolvidoPorId":%d,"itens":[{"itemEmpenhoId":%d,"condicao":"BOM"}],"observacao":"Fim do serviço"}
                                """.formatted(recebedor.getId(), itemEmpenhoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens[0].condicao").value("BOM"));

        mvc.perform(get("/api/v1/empenhos/abertos").with(operadorAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isEmpty());

        mvc.perform(get("/api/v1/itens/disponiveis").with(operadorAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroPatrimonio").value("PAT-HT-10"));
    }

    @Test
    void autenticaComMatriculaESenhaEDevolveFalhaGenerica() throws Exception {
        usuarios.save(new UsuarioJpaEntity(
                "Operador Basic", "BASIC-TESTE", CodificadorSenha.codificar("SenhaBasicTeste123!"),
                PerfilUsuario.OPERADOR_ESTOQUE, SETOR, "DIA"));

        mvc.perform(get("/api/v1/conta").with(httpBasic("BASIC-TESTE", "SenhaBasicTeste123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("BASIC-TESTE"));

        mvc.perform(get("/api/v1/conta").with(httpBasic("BASIC-TESTE", "senha-incorreta")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));

        mvc.perform(get("/api/v1/conta").with(httpBasic("MATRICULA-INEXISTENTE", "senha-incorreta")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    private static RequestPostProcessor autenticacao(Long id, PerfilUsuario perfil, String setor) {
        UsuarioAutenticado principal = new UsuarioAutenticado(
                id, "Usuário de teste", "MAT-TESTE", perfil, setor,
                perfil == PerfilUsuario.OPERADOR_ESTOQUE ? "DIA" : null);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
