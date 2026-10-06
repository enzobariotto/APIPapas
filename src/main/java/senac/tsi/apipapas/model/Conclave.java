package senac.tsi.apipapas.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Entity
public class Conclave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "6")
    private Long id;

    @NotNull(message = "A data de início é obrigatória")
    @PastOrPresent(message = "A data de início não pode estar no futuro")
    @Schema(example = "2013-03-12")
    private LocalDate dataInicio;

    @NotNull(message = "A data de término é obrigatória")
    @PastOrPresent(message = "A data de término não pode estar no futuro")
    @Schema(example = "2013-03-13")
    private LocalDate dataFim;

    @NotNull(message = "O número de cardeais é obrigatório")
    @Min(value = 1, message = "Deve haver pelo menos 1 cardeal")
    @Schema(description = "Cardeais com direito a voto", example = "115")
    private Integer numeroCardeais;

    @NotNull(message = "O número de escrutínios é obrigatório")
    @Min(value = 1, message = "Deve haver pelo menos 1 escrutínio")
    @Schema(description = "Quantidade de votações até a eleição", example = "5")
    private Integer numeroEscrutinios;

    @OneToOne
    @JoinColumn(name = "papa_id", unique = true)
    @NotNull(message = "O papa eleito é obrigatório")
    private Papa papa;

    public Conclave() {
    }

    public Conclave(LocalDate dataInicio, LocalDate dataFim, Integer numeroCardeais,
                    Integer numeroEscrutinios, Papa papa) {
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.numeroCardeais = numeroCardeais;
        this.numeroEscrutinios = numeroEscrutinios;
        this.papa = papa;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Integer getNumeroCardeais() {
        return numeroCardeais;
    }

    public void setNumeroCardeais(Integer numeroCardeais) {
        this.numeroCardeais = numeroCardeais;
    }

    public Integer getNumeroEscrutinios() {
        return numeroEscrutinios;
    }

    public void setNumeroEscrutinios(Integer numeroEscrutinios) {
        this.numeroEscrutinios = numeroEscrutinios;
    }

    public Papa getPapa() {
        return papa;
    }

    public void setPapa(Papa papa) {
        this.papa = papa;
    }
}
