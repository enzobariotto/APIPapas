package senac.tsi.apipapas.controller;

import senac.tsi.apipapas.exception.RecursoNaoEncontradoException;
import senac.tsi.apipapas.model.ChaveApi;
import senac.tsi.apipapas.repository.ChaveApiRepository;
import senac.tsi.apipapas.seguranca.ChaveApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/chaves-api")
@Tag(name = "Chaves de API", description = "Geração e gerenciamento das chaves de API dos usuários (exige a chave de admin)")
@SecurityRequirement(name = "ApiKey")
public class ChaveApiController {

    private final ChaveApiRepository repository;
    private final ChaveApiService service;
    private final PagedResourcesAssembler<ChaveApi> pagedAssembler;

    ChaveApiController(ChaveApiRepository repository, ChaveApiService service,
                       PagedResourcesAssembler<ChaveApi> pagedAssembler) {
        this.repository = repository;
        this.service = service;
        this.pagedAssembler = pagedAssembler;
    }

    public record NovaChaveApi(
            @NotBlank(message = "O nome é obrigatório")
            @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
            @Schema(description = "Nome do usuário ou aplicação que vai usar a chave", example = "Professor")
            String nome) {
    }

    public record ChaveApiGerada(
            Long id,
            String nome,
            String prefixo,
            @Schema(description = "Valor completo da chave. Guarde-o: ele não será mostrado novamente.")
            String chave,
            Instant criadaEm) {
    }

    @Operation(summary = "Gera uma chave de API para um usuário",
            description = "O valor completo da chave aparece somente nesta resposta. Envie-o no header X-API-Key.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Chave gerada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente", content = @Content),
            @ApiResponse(responseCode = "403", description = "A chave informada não é a de admin", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EntityModel<ChaveApiGerada>> gerar(@Valid @RequestBody NovaChaveApi dados) {
        ChaveApiService.ChaveGerada gerada = service.gerar(dados.nome());
        ChaveApi chave = gerada.chave();
        EntityModel<ChaveApiGerada> model = EntityModel.of(
                new ChaveApiGerada(chave.getId(), chave.getNome(), chave.getPrefixo(), gerada.valor(), chave.getCriadaEm()),
                linkTo(methodOn(ChaveApiController.class).buscar(chave.getId())).withSelfRel(),
                linkTo(methodOn(ChaveApiController.class).revogar(chave.getId())).withRel("revogar"),
                linkTo(ChaveApiController.class).withRel("chaves-api"));
        return ResponseEntity.created(linkTo(methodOn(ChaveApiController.class).buscar(chave.getId())).toUri()).body(model);
    }

    @Operation(summary = "Lista as chaves de API", description = "Mostra só o prefixo de cada chave, nunca o valor completo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de chaves"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente", content = @Content),
            @ApiResponse(responseCode = "403", description = "A chave informada não é a de admin", content = @Content)
    })
    @GetMapping
    public PagedModel<EntityModel<ChaveApi>> listar(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModel);
    }

    @Operation(summary = "Busca uma chave de API pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chave encontrada"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente", content = @Content),
            @ApiResponse(responseCode = "403", description = "A chave informada não é a de admin", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chave não encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public EntityModel<ChaveApi> buscar(@PathVariable Long id) {
        return toModel(buscarChave(id));
    }

    @Operation(summary = "Revoga uma chave de API",
            description = "A chave deixa de ser aceita imediatamente. O registro é mantido para histórico.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Chave revogada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente", content = @Content),
            @ApiResponse(responseCode = "403", description = "A chave informada não é a de admin", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chave não encontrada", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revogar(@PathVariable Long id) {
        ChaveApi chave = buscarChave(id);
        if (chave.isAtiva()) {
            chave.revogar();
            repository.save(chave);
        }
        return ResponseEntity.noContent().build();
    }

    private ChaveApi buscarChave(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("chave de API", id));
    }

    private EntityModel<ChaveApi> toModel(ChaveApi chave) {
        return EntityModel.of(chave,
                linkTo(methodOn(ChaveApiController.class).buscar(chave.getId())).withSelfRel(),
                linkTo(methodOn(ChaveApiController.class).revogar(chave.getId())).withRel("revogar"),
                linkTo(ChaveApiController.class).withRel("chaves-api"));
    }
}
