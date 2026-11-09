package senac.tsi.apipapas.controller;

import senac.tsi.apipapas.exception.RecursoNaoEncontradoException;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.model.PapaV2;
import senac.tsi.apipapas.model.SituacaoPontificado;
import senac.tsi.apipapas.repository.PapaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/papas")
@Tag(name = "Papas", description = "Cadastro dos papas da Igreja Católica")
public class PapaController {

    private final PapaRepository repository;
    private final PagedResourcesAssembler<Papa> pagedAssembler;

    PapaController(PapaRepository repository, PagedResourcesAssembler<Papa> pagedAssembler) {
        this.repository = repository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Lista os papas (versão 1)",
            description = "Versão padrão, usada quando o header X-API-Version não é enviado ou vale 1.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de papas")
    })
    @GetMapping(version = "1")
    public PagedModel<EntityModel<Papa>> listar(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModel);
    }

    @Operation(summary = "Lista os papas (versão 2)",
            description = "Enviar X-API-Version: 2. Agrupa nome e pontificado e inclui duracaoDias e emExercicio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de papas no formato da versão 2")
    })
    @GetMapping(version = "2")
    public PagedModel<EntityModel<PapaV2>> listarV2(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModelV2);
    }

    @Operation(summary = "Busca um papa pelo id (versão 1)",
            description = "Versão padrão, usada quando o header X-API-Version não é enviado ou vale 1.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papa encontrado"),
            @ApiResponse(responseCode = "404", description = "Papa não encontrado", content = @Content)
    })
    @GetMapping(path = "/{id}", version = "1")
    public EntityModel<Papa> buscar(@Parameter(description = "Id do(a) papa", example = "6") @PathVariable Long id) {
        return toModel(buscarPapa(id));
    }

    @Operation(summary = "Busca um papa pelo id (versão 2)",
            description = "Enviar X-API-Version: 2. Agrupa nome e pontificado e inclui duracaoDias e emExercicio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papa encontrado, no formato da versão 2"),
            @ApiResponse(responseCode = "404", description = "Papa não encontrado", content = @Content)
    })
    @GetMapping(path = "/{id}", version = "2")
    public EntityModel<PapaV2> buscarV2(@Parameter(description = "Id do(a) papa", example = "6") @PathVariable Long id) {
        return toModelV2(buscarPapa(id));
    }

    @Operation(summary = "Cadastra um papa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Papa cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EntityModel<Papa>> criar(@Valid @RequestBody Papa novoPapa) {
        novoPapa.setId(null);
        Papa salvo = repository.save(novoPapa);
        EntityModel<Papa> model = toModel(salvo);
        return ResponseEntity.created(linkTo(methodOn(PapaController.class).buscar(salvo.getId())).toUri()).body(model);
    }

    @Operation(summary = "Atualiza um papa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papa atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Papa não encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public EntityModel<Papa> atualizar(@Parameter(description = "Id do(a) papa", example = "6") @PathVariable Long id, @Valid @RequestBody Papa dados) {
        Papa papa = buscarPapa(id);
        papa.setNomePapal(dados.getNomePapal());
        papa.setNomeBatismo(dados.getNomeBatismo());
        papa.setNumeroOrdem(dados.getNumeroOrdem());
        papa.setPaisNascimento(dados.getPaisNascimento());
        papa.setInicioPontificado(dados.getInicioPontificado());
        papa.setFimPontificado(dados.getFimPontificado());
        papa.setSituacao(dados.getSituacao());
        return toModel(repository.save(papa));
    }

    @Operation(summary = "Exclui um papa")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Papa excluído", content = @Content),
            @ApiResponse(responseCode = "404", description = "Papa não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Papa possui registros vinculados (conclave, encíclicas, concílios ou santos)", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "Id do(a) papa", example = "6") @PathVariable Long id) {
        repository.delete(buscarPapa(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Busca papas pela situação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de papas com a situação informada"),
            @ApiResponse(responseCode = "400", description = "Situação inválida", content = @Content)
    })
    @GetMapping("/situacao/{situacao}")
    public PagedModel<EntityModel<Papa>> buscarPorSituacao(@Parameter(description = "Situação do pontificado") @PathVariable SituacaoPontificado situacao,
                                                           @ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findBySituacao(situacao, pageable), this::toModel);
    }

    private Papa buscarPapa(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("papa", id));
    }

    private EntityModel<PapaV2> toModelV2(Papa papa) {
        return EntityModel.of(PapaV2.de(papa),
                linkTo(methodOn(PapaController.class).buscar(papa.getId())).withSelfRel(),
                linkTo(methodOn(PapaController.class).atualizar(papa.getId(), null)).withRel("update"),
                linkTo(methodOn(PapaController.class).excluir(papa.getId())).withRel("delete"),
                linkTo(PapaController.class).withRel("papas"),
                linkTo(EnciclicaController.class).slash("papa").slash(papa.getId()).withRel("enciclicas"));
    }

    private EntityModel<Papa> toModel(Papa papa) {
        return EntityModel.of(papa,
                linkTo(methodOn(PapaController.class).buscar(papa.getId())).withSelfRel(),
                linkTo(methodOn(PapaController.class).atualizar(papa.getId(), null)).withRel("update"),
                linkTo(methodOn(PapaController.class).excluir(papa.getId())).withRel("delete"),
                linkTo(PapaController.class).withRel("papas"),
                linkTo(EnciclicaController.class).slash("papa").slash(papa.getId()).withRel("enciclicas"));
    }
}
