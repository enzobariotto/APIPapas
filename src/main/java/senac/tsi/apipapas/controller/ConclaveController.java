package senac.tsi.apipapas.controller;

import senac.tsi.apipapas.exception.RecursoNaoEncontradoException;
import senac.tsi.apipapas.model.Conclave;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.repository.ConclaveRepository;
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
@RequestMapping("/conclaves")
@Tag(name = "Conclaves", description = "Conclaves que elegeram os papas (One-to-One com Papa)")
public class ConclaveController {

    private final ConclaveRepository repository;
    private final PapaRepository papaRepository;
    private final PagedResourcesAssembler<Conclave> pagedAssembler;

    ConclaveController(ConclaveRepository repository, PapaRepository papaRepository,
                       PagedResourcesAssembler<Conclave> pagedAssembler) {
        this.repository = repository;
        this.papaRepository = papaRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Lista os conclaves")
    @GetMapping
    public PagedModel<EntityModel<Conclave>> listar(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModel);
    }

    @Operation(summary = "Busca um conclave pelo id")
    @GetMapping("/{id}")
    public EntityModel<Conclave> buscar(@PathVariable Long id) {
        Conclave conclave = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("conclave", id));
        return toModel(conclave);
    }

    @Operation(summary = "Cadastra um conclave")
    @PostMapping
    public ResponseEntity<EntityModel<Conclave>> criar(@Valid @RequestBody Conclave novoConclave) {
        novoConclave.setId(null);
        novoConclave.setPapa(buscarPapa(novoConclave.getPapa().getId()));
        Conclave salvo = repository.save(novoConclave);
        return ResponseEntity.created(linkTo(methodOn(ConclaveController.class).buscar(salvo.getId())).toUri())
                .body(toModel(salvo));
    }

    @Operation(summary = "Atualiza um conclave")
    @PutMapping("/{id}")
    public EntityModel<Conclave> atualizar(@PathVariable Long id, @Valid @RequestBody Conclave dados) {
        Conclave conclave = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("conclave", id));
        conclave.setDataInicio(dados.getDataInicio());
        conclave.setDataFim(dados.getDataFim());
        conclave.setNumeroCardeais(dados.getNumeroCardeais());
        conclave.setNumeroEscrutinios(dados.getNumeroEscrutinios());
        conclave.setPapa(buscarPapa(dados.getPapa().getId()));
        return toModel(repository.save(conclave));
    }

    @Operation(summary = "Exclui um conclave")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("conclave", id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Busca conclaves rápidos")
    @GetMapping("/rapidos")
    public PagedModel<EntityModel<Conclave>> buscarRapidos(@RequestParam Integer maxEscrutinios,
                                                           @ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findByNumeroEscrutiniosLessThanEqual(maxEscrutinios, pageable), this::toModel);
    }

    private Papa buscarPapa(Long papaId) {
        return papaRepository.findById(papaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("papa", papaId));
    }

    private EntityModel<Conclave> toModel(Conclave conclave) {
        return EntityModel.of(conclave,
                linkTo(methodOn(ConclaveController.class).buscar(conclave.getId())).withSelfRel(),
                linkTo(methodOn(ConclaveController.class).atualizar(conclave.getId(), null)).withRel("update"),
                linkTo(methodOn(ConclaveController.class).excluir(conclave.getId())).withRel("delete"),
                linkTo(ConclaveController.class).withRel("conclaves"),
                linkTo(methodOn(PapaController.class).buscar(conclave.getPapa().getId())).withRel("papa"));
    }
}
