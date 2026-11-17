package senac.tsi.apipapas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import senac.tsi.apipapas.seguranca.Planos;

import java.time.Instant;
import java.time.YearMonth;

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

    @Schema(description = "Plano da chave: define o limite por minuto e a cota mensal (ver GET /planos)", example = "BASICO")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plano plano = Plano.GRATUITO;

    /** Mês (AAAA-MM) a que se refere o contador requisicoes. Ao virar o mês, o contador volta a zero. */
    @JsonIgnore
    private String mesReferencia;

    @JsonIgnore
    private long requisicoes;

    public ChaveApi() {
    }

    public ChaveApi(String nome, String prefixo, String hash, Plano plano) {
        this.nome = nome;
        this.prefixo = prefixo;
        this.hash = hash;
        this.plano = plano == null ? Plano.GRATUITO : plano;
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

    public Plano getPlano() {
        return plano;
    }

    public void setPlano(Plano plano) {
        this.plano = plano;
    }

    @Schema(description = "Requisições feitas com a chave no mês atual (a cota renova no dia 1º)", example = "1234")
    public long getRequisicoesNoMes() {
        return Planos.mesAtual().toString().equals(mesReferencia) ? requisicoes : 0;
    }

    /**
     * Conta mais uma requisição no mês atual, se ainda houver cota.
     *
     * @return false quando a cota do mês já foi toda usada (a requisição não é contada)
     */
    public boolean consumirCota(long cotaMensal) {
        YearMonth mes = Planos.mesAtual();
        if (!mes.toString().equals(mesReferencia)) {
            mesReferencia = mes.toString();
            requisicoes = 0;
        }
        if (requisicoes >= cotaMensal) {
            return false;
        }
        requisicoes++;
        return true;
    }
}
