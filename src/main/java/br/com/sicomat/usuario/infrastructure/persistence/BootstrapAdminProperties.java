package br.com.sicomat.usuario.infrastructure.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sicomat.bootstrap.admin")
public record BootstrapAdminProperties(String nome, String matricula, String senha, String setor) {
}