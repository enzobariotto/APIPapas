package senac.tsi.apipapas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class ChaveApi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Nome do usuário ou aplicação dona da chave", example = "Professor")
    private String nome;

    @Schema(description = "Início da chave, para identificá-la sem expor o valor completo", example = "papas_Ab3dE")
    private String prefixo;

    /** Só o hash SHA-256 da chave é guardado; o valor completo é mostrado uma única vez, na geração. */
    @JsonIgnore
    @Column(unique = true, nullable = false)
    private String hash;

    private boolean ativa = true;

    private Instant criadaEm;

    private Instant ultimoUsoEm;

    private Instant revogadaEm;

    public ChaveApi() {
    }

    public ChaveApi(String nome, String prefixo, String hash) {
        this.nome = nome;
        this.prefixo = prefixo;
        this.hash = hash;
        this.criadaEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getPrefixo() {
        return prefixo;
    }

    public String getHash() {
        return hash;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getUltimoUsoEm() {
        return ultimoUsoEm;
    }

    public void setUltimoUsoEm(Instant ultimoUsoEm) {
        this.ultimoUsoEm = ultimoUsoEm;
    }

    public Instant getRevogadaEm() {
        return revogadaEm;
    }

    public void revogar() {
        this.ativa = false;
        this.revogadaEm = Instant.now();
    }
}
