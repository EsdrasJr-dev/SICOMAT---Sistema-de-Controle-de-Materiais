package br.com.sicomat.usuario.infrastructure.persistence;

import br.com.sicomat.seguranca.CodificadorSenha;
import br.com.sicomat.usuario.PerfilUsuario;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class BootstrapAdminConfiguration {

    @Bean
    ApplicationRunner criarAdministradorInicial(UsuarioJpaRepository usuarios, BootstrapAdminProperties propriedades) {
        return args -> {
            long administradores = usuarios.countByPerfil(PerfilUsuario.ADMINISTRADOR_SISTEMA);
            if (administradores > 1) {
                throw new IllegalStateException("O banco contém mais de um administrador do sistema.");
            }
            if (administradores == 1) return;

            exigirConfigurado(propriedades.nome(), "SICOMAT_ADMIN_NOME");
            exigirConfigurado(propriedades.matricula(), "SICOMAT_ADMIN_MATRICULA");
            exigirConfigurado(propriedades.senha(), "SICOMAT_ADMIN_SENHA");
            exigirConfigurado(propriedades.setor(), "SICOMAT_ADMIN_SETOR");

            if (usuarios.existsByMatriculaIgnoreCase(propriedades.matricula().trim())) {
                throw new IllegalStateException("A matrícula configurada para o administrador inicial já pertence a outra conta.");
            }

            UsuarioJpaEntity administrador = new UsuarioJpaEntity(
                    propriedades.nome().trim(),
                    propriedades.matricula().trim(),
                    CodificadorSenha.codificar(propriedades.senha()),
                    PerfilUsuario.ADMINISTRADOR_SISTEMA,
                    propriedades.setor().trim(),
                    null);
            usuarios.save(administrador);
        };
    }

    private static void exigirConfigurado(String valor, String variavel) {
        if (valor == null || valor.isBlank() || valor.startsWith("SUBSTITUA_") || variavel.equals("SICOMAT_ADMIN_SENHA") && valor.length() < 8) {
            throw new IllegalStateException("Configure " + variavel + " antes de iniciar o SICOMAT.");
        }
    }
}