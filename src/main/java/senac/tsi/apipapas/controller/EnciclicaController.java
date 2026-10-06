package senac.tsi.apipapas.controller;

import senac.tsi.apipapas.exception.RecursoNaoEncontradoException;
import senac.tsi.apipapas.model.Enciclica;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.repository.EnciclicaRepository;
import senac.tsi.apipapas.repository.PapaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/enciclicas")
@Tag(name = "Encíclicas", description = "Encíclicas escritas pelos papas (One-to-Many com Papa)")
public class EnciclicaController {

    private final EnciclicaRepository repository;
    private final PapaRepository papaRepository;
    private final PagedResourcesAssembler<Enciclica> pagedAssembler;

    EnciclicaController(EnciclicaRepository repository, PapaRepository papaRepository,
                        PagedResourcesAssembler<Enciclica> pagedAssembler) {
        this.repository = repository;
        this.papaRepository = papaRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Lista as encíclicas")
    @GetMapping
    public PagedModel<EntityModel<Enciclica>> listar(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModel);
    }

    @Operation(summary = "Busca uma encíclica pelo id")
    @GetMapping("/{id}")
    public EntityModel<Enciclica> buscar(@PathVariable Long id) {
        Enciclica enciclica = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("encíclica", id));
        return toModel(enciclica);
    }

    @Operation(summary = "Cadastra uma encíclica")
    @PostMapping
    public ResponseEntity<EntityModel<Enciclica>> criar(@Valid @RequestBody Enciclica novaEnciclica) {
        novaEnciclica.setId(null);
        novaEnciclica.setPapa(buscarPapa(novaEnciclica.getPapa().getId()));
        Enciclica salva = repository.save(novaEnciclica);
        return ResponseEntity.created(linkTo(methodOn(EnciclicaController.class).buscar(salva.getId())).toUri())
                .body(toModel(salva));
    }

    @Operation(summary = "Atualiza uma encíclica")
    @PutMapping("/{id}")
    public EntityModel<Enciclica> atualizar(@PathVariable Long id, @Valid @RequestBody Enciclica dados) {
        Enciclica enciclica = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("encíclica", id));
        enciclica.setTitulo(dados.getTitulo());
        enciclica.setTituloPortugues(dados.getTituloPortugues());
        enciclica.setDataPublicacao(dados.getDataPublicacao());
        enciclica.setPapa(buscarPapa(dados.getPapa().getId()));
        return toModel(repository.save(enciclica));
    }

    @Operation(summary = "Exclui uma encíclica")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("encíclica", id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lista as encíclicas de um papa")
    @GetMapping("/papa/{papaId}")
    public PagedModel<EntityModel<Enciclica>> buscarPorPapa(@PathVariable Long papaId,
                                                            @ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findByPapaId(papaId, pageable), this::toModel);
    }

    private Papa buscarPapa(Long papaId) {
        return papaRepository.findById(papaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("papa", papaId));
    }

    private EntityModel<Enciclica> toModel(Enciclica enciclica) {
        return EntityModel.of(enciclica,
                linkTo(methodOn(EnciclicaController.class).buscar(enciclica.getId())).withSelfRel(),
                linkTo(methodOn(EnciclicaController.class).atualizar(enciclica.getId(), null)).withRel("update"),
                linkTo(methodOn(EnciclicaController.class).excluir(enciclica.getId())).withRel("delete"),
                linkTo(EnciclicaController.class).withRel("enciclicas"),
                linkTo(methodOn(PapaController.class).buscar(enciclica.getPapa().getId())).withRel("papa"));
    }
}
