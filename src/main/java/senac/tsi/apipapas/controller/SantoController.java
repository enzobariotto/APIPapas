package senac.tsi.apipapas.controller;

import senac.tsi.apipapas.exception.RecursoNaoEncontradoException;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.model.Santo;
import senac.tsi.apipapas.repository.PapaRepository;
import senac.tsi.apipapas.repository.SantoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/santos")
@Tag(name = "Santos", description = "Santos canonizados pelos papas (Many-to-One com Papa)")
public class SantoController {

    private final SantoRepository repository;
    private final PapaRepository papaRepository;
    private final PagedResourcesAssembler<Santo> pagedAssembler;

    SantoController(SantoRepository repository, PapaRepository papaRepository,
                    PagedResourcesAssembler<Santo> pagedAssembler) {
        this.repository = repository;
        this.papaRepository = papaRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Lista os santos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de santos")
    })
    @GetMapping
    public PagedModel<EntityModel<Santo>> listar(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModel);
    }

    @Operation(summary = "Busca um santo pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Santo encontrado"),
            @ApiResponse(responseCode = "404", description = "Santo não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public EntityModel<Santo> buscar(@PathVariable Long id) {
        return toModel(buscarSanto(id));
    }

    @Operation(summary = "Cadastra um santo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Santo cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Papa não encontrado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EntityModel<Santo>> criar(@Valid @RequestBody Santo novoSanto) {
        novoSanto.setId(null);
        novoSanto.setCanonizadoPor(buscarPapa(novoSanto.getCanonizadoPor().getId()));
        Santo salvo = repository.save(novoSanto);
        return ResponseEntity.created(linkTo(methodOn(SantoController.class).buscar(salvo.getId())).toUri())
                .body(toModel(salvo));
    }

    @Operation(summary = "Atualiza um santo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Santo atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Santo ou papa não encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public EntityModel<Santo> atualizar(@PathVariable Long id, @Valid @RequestBody Santo dados) {
        Santo santo = buscarSanto(id);
        santo.setNome(dados.getNome());
        santo.setPaisOrigem(dados.getPaisOrigem());
        santo.setDataCanonizacao(dados.getDataCanonizacao());
        santo.setCanonizadoPor(buscarPapa(dados.getCanonizadoPor().getId()));
        return toModel(repository.save(santo));
    }

    @Operation(summary = "Exclui um santo")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Santo excluído", content = @Content),
            @ApiResponse(responseCode = "404", description = "Santo não encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        repository.delete(buscarSanto(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Busca santos por país de origem")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de santos do país informado")
    })
    @GetMapping("/pais/{pais}")
    public PagedModel<EntityModel<Santo>> buscarPorPais(@PathVariable String pais,
                                                        @ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findByPaisOrigemIgnoreCase(pais, pageable), this::toModel);
    }

    private Santo buscarSanto(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("santo", id));
    }

    private Papa buscarPapa(Long papaId) {
        return papaRepository.findById(papaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("papa", papaId));
    }

    private EntityModel<Santo> toModel(Santo santo) {
        return EntityModel.of(santo,
                linkTo(methodOn(SantoController.class).buscar(santo.getId())).withSelfRel(),
                linkTo(methodOn(SantoController.class).atualizar(santo.getId(), null)).withRel("update"),
                linkTo(methodOn(SantoController.class).excluir(santo.getId())).withRel("delete"),
                linkTo(SantoController.class).withRel("santos"),
                linkTo(methodOn(PapaController.class).buscar(santo.getCanonizadoPor().getId())).withRel("papa"));
    }
}
