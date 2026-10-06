package senac.tsi.apipapas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Papa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "O nome papal é obrigatório")
    @Schema(example = "Francisco")
    private String nomePapal;

    @NotBlank(message = "O nome de batismo é obrigatório")
    @Schema(example = "Jorge Mario Bergoglio")
    private String nomeBatismo;

    @NotNull(message = "O número de ordem é obrigatório")
    @Positive(message = "O número de ordem deve ser positivo")
    @Schema(description = "Posição na lista de papas (São Pedro = 1)", example = "266")
    private Integer numeroOrdem;

    @NotBlank(message = "O país de nascimento é obrigatório")
    @Schema(example = "Argentina")
    private String paisNascimento;

    @NotNull(message = "A data de início do pontificado é obrigatória")
    @PastOrPresent(message = "A data de início não pode estar no futuro")
    @Schema(example = "2013-03-13")
    private LocalDate inicioPontificado;

    @PastOrPresent(message = "A data de fim não pode estar no futuro")
    @Schema(description = "Vazio se o papa ainda está em exercício", example = "2025-04-21")
    private LocalDate fimPontificado;

    @NotNull(message = "A situação é obrigatória")
    @Enumerated(EnumType.STRING)
    @Schema(example = "FALECIMENTO")
    private SituacaoPontificado situacao;

    @OneToMany(mappedBy = "papa")
    @JsonIgnore
    private List<Enciclica> enciclicas = new ArrayList<>();

    public Papa() {
    }

    public Papa(String nomePapal, String nomeBatismo, Integer numeroOrdem, String paisNascimento,
                LocalDate inicioPontificado, LocalDate fimPontificado, SituacaoPontificado situacao) {
        this.nomePapal = nomePapal;
        this.nomeBatismo = nomeBatismo;
        this.numeroOrdem = numeroOrdem;
        this.paisNascimento = paisNascimento;
        this.inicioPontificado = inicioPontificado;
        this.fimPontificado = fimPontificado;
        this.situacao = situacao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomePapal() {
        return nomePapal;
    }

    public void setNomePapal(String nomePapal) {
        this.nomePapal = nomePapal;
    }

    public String getNomeBatismo() {
        return nomeBatismo;
    }

    public void setNomeBatismo(String nomeBatismo) {
        this.nomeBatismo = nomeBatismo;
    }

    public Integer getNumeroOrdem() {
        return numeroOrdem;
    }

    public void setNumeroOrdem(Integer numeroOrdem) {
        this.numeroOrdem = numeroOrdem;
    }

    public String getPaisNascimento() {
        return paisNascimento;
    }

    public void setPaisNascimento(String paisNascimento) {
        this.paisNascimento = paisNascimento;
    }

    public LocalDate getInicioPontificado() {
        return inicioPontificado;
    }

    public void setInicioPontificado(LocalDate inicioPontificado) {
        this.inicioPontificado = inicioPontificado;
    }

    public LocalDate getFimPontificado() {
        return fimPontificado;
    }

    public void setFimPontificado(LocalDate fimPontificado) {
        this.fimPontificado = fimPontificado;
    }

    public SituacaoPontificado getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoPontificado situacao) {
        this.situacao = situacao;
    }

    public List<Enciclica> getEnciclicas() {
        return enciclicas;
    }
}
