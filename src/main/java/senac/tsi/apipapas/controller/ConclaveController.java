package senac.tsi.apipapas.controller;

import senac.tsi.apipapas.exception.RecursoNaoEncontradoException;
import senac.tsi.apipapas.model.Conclave;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.repository.ConclaveRepository;
import senac.tsi.apipapas.repository.PapaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import senac.tsi.apipapas.exception.DadosInvalidosException;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de conclaves")
    })
    @GetMapping
    public PagedModel<EntityModel<Conclave>> listar(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModel);
    }

    @Operation(summary = "Busca um conclave pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conclave encontrado"),
            @ApiResponse(responseCode = "404", description = "Conclave não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public EntityModel<Conclave> buscar(@Parameter(description = "Id do(a) conclave", example = "1") @PathVariable Long id) {
        return toModel(buscarConclave(id));
    }

    @Operation(summary = "Cadastra um conclave")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conclave cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Papa não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O papa informado já possui um conclave", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EntityModel<Conclave>> criar(@Valid @RequestBody Conclave novoConclave) {
        novoConclave.setId(null);
        novoConclave.setPapa(buscarPapa(novoConclave.getPapa().getId()));
        Conclave salvo = repository.save(novoConclave);
        return ResponseEntity.created(linkTo(methodOn(ConclaveController.class).buscar(salvo.getId())).toUri())
                .body(toModel(salvo));
    }

    @Operation(summary = "Atualiza um conclave")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conclave atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Conclave ou papa não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O papa informado já possui um conclave", content = @Content)
    })
    @PutMapping("/{id}")
    public EntityModel<Conclave> atualizar(@Parameter(description = "Id do(a) conclave", example = "1") @PathVariable Long id, @Valid @RequestBody Conclave dados) {
        Conclave conclave = buscarConclave(id);
        conclave.setDataInicio(dados.getDataInicio());
        conclave.setDataFim(dados.getDataFim());
        conclave.setNumeroCardeais(dados.getNumeroCardeais());
        conclave.setNumeroEscrutinios(dados.getNumeroEscrutinios());
        conclave.setPapa(buscarPapa(dados.getPapa().getId()));
        return toModel(repository.save(conclave));
    }

    @Operation(summary = "Exclui um conclave")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conclave excluído", content = @Content),
            @ApiResponse(responseCode = "404", description = "Conclave não encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "Id do(a) conclave", example = "1") @PathVariable Long id) {
        repository.delete(buscarConclave(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Busca conclaves rápidos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de conclaves com até o número de escrutínios informado"),
            @ApiResponse(responseCode = "400", description = "Parâmetro maxEscrutinios ausente ou inválido", content = @Content)
    })
    @GetMapping("/rapidos")
    public PagedModel<EntityModel<Conclave>> buscarRapidos(@Parameter(description = "Número máximo de escrutínios (mínimo 1)", example = "4")
                                                           @RequestParam @Min(value = 1, message = "Deve ser maior ou igual a 1") Integer maxEscrutinios,
                                                           @ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findByNumeroEscrutiniosLessThanEqual(maxEscrutinios, pageable), this::toModel);
    }

    private Conclave buscarConclave(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("conclave", id));
    }

    private Papa buscarPapa(Long papaId) {
        if (papaId == null) {
            throw new DadosInvalidosException("papa.id", "Informe o id do papa eleito, ex.: {\"id\": 3}");
        }
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
