package senac.tsi.apipapas.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Entity
public class Santo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Schema(example = "Santa Dulce dos Pobres")
    private String nome;

    @NotBlank(message = "O país de origem é obrigatório")
    @Schema(example = "Brasil")
    private String paisOrigem;

    @NotNull(message = "A data de canonização é obrigatória")
    @PastOrPresent(message = "A data de canonização não pode estar no futuro")
    @Schema(example = "2019-10-13")
    private LocalDate dataCanonizacao;

    @ManyToOne
    @NotNull(message = "O papa que canonizou é obrigatório")
    @Schema(example = "{\"id\": 6}")
    private Papa canonizadoPor;

    public Santo() {
    }

    public Santo(String nome, String paisOrigem, LocalDate dataCanonizacao, Papa canonizadoPor) {
        this.nome = nome;
        this.paisOrigem = paisOrigem;
        this.dataCanonizacao = dataCanonizacao;
        this.canonizadoPor = canonizadoPor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPaisOrigem() {
        return paisOrigem;
    }

    public void setPaisOrigem(String paisOrigem) {
        this.paisOrigem = paisOrigem;
    }

    public LocalDate getDataCanonizacao() {
        return dataCanonizacao;
    }

    public void setDataCanonizacao(LocalDate dataCanonizacao) {
        this.dataCanonizacao = dataCanonizacao;
    }

    public Papa getCanonizadoPor() {
        return canonizadoPor;
    }

    public void setCanonizadoPor(Papa canonizadoPor) {
        this.canonizadoPor = canonizadoPor;
    }
}
