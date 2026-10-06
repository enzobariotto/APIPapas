package senac.tsi.apipapas.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Concilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Schema(example = "Concílio Vaticano II")
    private String nome;

    @NotBlank(message = "O local é obrigatório")
    @Schema(example = "Basílica de São Pedro, Vaticano")
    private String local;

    @NotNull(message = "A data de início é obrigatória")
    @Past(message = "A data de início deve estar no passado")
    @Schema(example = "1962-10-11")
    private LocalDate dataInicio;

    @NotNull(message = "A data de encerramento é obrigatória")
    @PastOrPresent(message = "A data de encerramento não pode estar no futuro")
    @Schema(example = "1965-12-08")
    private LocalDate dataFim;

    @ManyToMany
    @JoinTable(name = "concilio_papa",
            joinColumns = @JoinColumn(name = "concilio_id"),
            inverseJoinColumns = @JoinColumn(name = "papa_id"))
    @NotEmpty(message = "Informe pelo menos um papa")
    @Schema(example = "[{\"id\": 2}, {\"id\": 3}]")
    private List<Papa> papas = new ArrayList<>();

    public Concilio() {
    }

    public Concilio(String nome, String local, LocalDate dataInicio, LocalDate dataFim, List<Papa> papas) {
        this.nome = nome;
        this.local = local;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.papas = papas;
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

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
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

    public List<Papa> getPapas() {
        return papas;
    }

    public void setPapas(List<Papa> papas) {
        this.papas = papas;
    }
}
