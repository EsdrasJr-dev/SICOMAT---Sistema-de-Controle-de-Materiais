package br.com.sicomat.seguranca;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Cria e verifica hashes de senha usando PBKDF2, disponível no JDK. */
public final class CodificadorSenha {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIXO = "pbkdf2_sha256";
    private static final int ITERACOES = 210_000;
    private static final int TAMANHO_SALT = 16;
    private static final int TAMANHO_HASH_BITS = 256;
    private static final int ITERACOES_MINIMAS = 10_000;
    private static final int ITERACOES_MAXIMAS = 1_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodificadorSenha() {
    }

    public static String codificar(String senha) {
        validarSenha(senha);
        byte[] salt = new byte[TAMANHO_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = derivar(senha.toCharArray(), salt, ITERACOES);
        return PREFIXO + "$" + ITERACOES + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(hash);
    }

    public static boolean verificar(String senha, String hashArmazenado) {
        if (senha == null || hashArmazenado == null) {
            return false;
        }

        try {
            String[] partes = hashArmazenado.split("\\$", -1);
            if (partes.length != 4 || !PREFIXO.equals(partes[0])) {
                return false;
            }
            int iteracoes = Integer.parseInt(partes[1]);
            if (iteracoes < ITERACOES_MINIMAS || iteracoes > ITERACOES_MAXIMAS) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] hashEsperado = Base64.getDecoder().decode(partes[3]);
            if (salt.length < 16 || hashEsperado.length != TAMANHO_HASH_BITS / 8) {
                return false;
            }
            byte[] hashRecebido = derivar(senha.toCharArray(), salt, iteracoes);
            return MessageDigest.isEqual(hashEsperado, hashRecebido);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("A senha não pode ficar vazia.");
        }
    }

    private static byte[] derivar(char[] senha, byte[] salt, int iteracoes) {
        char[] copiaSenha = Arrays.copyOf(senha, senha.length);
        PBEKeySpec spec = new PBEKeySpec(copiaSenha, salt, iteracoes, TAMANHO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Não foi possível processar o hash da senha.", ex);
        } finally {
            spec.clearPassword();
            Arrays.fill(copiaSenha, '\0');
            Arrays.fill(senha, '\0');
        }
    }
}

