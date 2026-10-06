package senac.tsi.apipapas.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Entity
public class Enciclica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(example = "1")
    private Long id;

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
    @Schema(description = "Título original em latim", example = "Laudato Si'")
    private String titulo;

    @Size(max = 150, message = "A tradução deve ter no máximo 150 caracteres")
    @Schema(example = "Louvado Sejas")
    private String tituloPortugues;

    @NotNull(message = "A data de publicação é obrigatória")
    @PastOrPresent(message = "A data de publicação não pode estar no futuro")
    @Schema(example = "2015-05-24")
    private LocalDate dataPublicacao;

    @ManyToOne
    @NotNull(message = "O papa é obrigatório")
    private Papa papa;

    public Enciclica() {
    }

    public Enciclica(String titulo, String tituloPortugues, LocalDate dataPublicacao, Papa papa) {
        this.titulo = titulo;
        this.tituloPortugues = tituloPortugues;
        this.dataPublicacao = dataPublicacao;
        this.papa = papa;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTituloPortugues() {
        return tituloPortugues;
    }

    public void setTituloPortugues(String tituloPortugues) {
        this.tituloPortugues = tituloPortugues;
    }

    public LocalDate getDataPublicacao() {
        return dataPublicacao;
    }

    public void setDataPublicacao(LocalDate dataPublicacao) {
        this.dataPublicacao = dataPublicacao;
    }

    public Papa getPapa() {
        return papa;
    }

    public void setPapa(Papa papa) {
        this.papa = papa;
    }
}
