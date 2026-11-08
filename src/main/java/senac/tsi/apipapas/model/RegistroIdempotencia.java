package senac.tsi.apipapas.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"cliente", "chave"}))
public class RegistroIdempotencia {

    public enum Situacao { PROCESSANDO, CONCLUIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Quem fez a requisição ("admin" ou "chave-{id}"): cada cliente tem o seu próprio espaço de chaves. */
    @Column(nullable = false)
    private String cliente;

    @Column(nullable = false, length = 100)
    private String chave;

    /** Hash do método + caminho + corpo, para detectar reuso da chave com outra requisição. */
    @Column(nullable = false)
    private String hashRequisicao;

    @Enumerated(EnumType.STRING)
    private Situacao situacao;

    private Integer statusHttp;

    private String contentType;

    private String location;

    @Lob
    private String corpoResposta;

    private Instant criadoEm;

    public RegistroIdempotencia() {
    }

    public RegistroIdempotencia(String cliente, String chave, String hashRequisicao) {
        this.cliente = cliente;
        this.chave = chave;
        this.hashRequisicao = hashRequisicao;
        this.situacao = Situacao.PROCESSANDO;
        this.criadoEm = Instant.now();
    }

    public void concluir(int statusHttp, String contentType, String location, String corpoResposta) {
        this.situacao = Situacao.CONCLUIDO;
        this.statusHttp = statusHttp;
        this.contentType = contentType;
        this.location = location;
        this.corpoResposta = corpoResposta;
    }

    public Long getId() {
        return id;
    }

    public String getHashRequisicao() {
        return hashRequisicao;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public Integer getStatusHttp() {
        return statusHttp;
    }

    public String getContentType() {
        return contentType;
    }

    public String getLocation() {
        return location;
    }

    public String getCorpoResposta() {
        return corpoResposta;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
