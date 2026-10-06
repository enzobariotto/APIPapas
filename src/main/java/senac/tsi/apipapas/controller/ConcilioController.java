package senac.tsi.apipapas.controller;

import senac.tsi.apipapas.exception.RecursoNaoEncontradoException;
import senac.tsi.apipapas.model.Concilio;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.repository.ConcilioRepository;
import senac.tsi.apipapas.repository.PapaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/concilios")
@Tag(name = "Concílios", description = "Concílios ecumênicos (Many-to-Many com Papa)")
public class ConcilioController {

    private final ConcilioRepository repository;
    private final PapaRepository papaRepository;
    private final PagedResourcesAssembler<Concilio> pagedAssembler;

    ConcilioController(ConcilioRepository repository, PapaRepository papaRepository,
                       PagedResourcesAssembler<Concilio> pagedAssembler) {
        this.repository = repository;
        this.papaRepository = papaRepository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Lista os concílios", description = "Retorna os concílios paginados, com os papas de cada um.")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public PagedModel<EntityModel<Concilio>> listar(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModel);
    }

    @Operation(summary = "Busca um concílio pelo id")
    @ApiResponse(responseCode = "200", description = "Concílio encontrado")
    @ApiResponse(responseCode = "404", description = "Concílio não encontrado")
    @GetMapping("/{id}")
    public EntityModel<Concilio> buscar(@PathVariable Long id) {
        Concilio concilio = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("concílio", id));
        return toModel(concilio);
    }

    @Operation(summary = "Cadastra um concílio",
            description = "Informe os papas pelo id. Ex.: \"papas\": [{\"id\": 2}, {\"id\": 3}]")
    @ApiResponse(responseCode = "201", description = "Concílio criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Algum papa não foi encontrado")
    @PostMapping
    public ResponseEntity<EntityModel<Concilio>> criar(@Valid @RequestBody Concilio novoConcilio) {
        novoConcilio.setPapas(buscarPapas(novoConcilio.getPapas()));
        Concilio salvo = repository.save(novoConcilio);
        return ResponseEntity.created(linkTo(methodOn(ConcilioController.class).buscar(salvo.getId())).toUri())
                .body(toModel(salvo));
    }

    @Operation(summary = "Atualiza um concílio")
    @ApiResponse(responseCode = "200", description = "Concílio atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Concílio ou papa não encontrado")
    @PutMapping("/{id}")
    public EntityModel<Concilio> atualizar(@PathVariable Long id, @Valid @RequestBody Concilio dados) {
        Concilio concilio = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("concílio", id));
        concilio.setNome(dados.getNome());
        concilio.setLocal(dados.getLocal());
        concilio.setDataInicio(dados.getDataInicio());
        concilio.setDataFim(dados.getDataFim());
        concilio.setPapas(buscarPapas(dados.getPapas()));
        return toModel(repository.save(concilio));
    }

    @Operation(summary = "Exclui um concílio")
    @ApiResponse(responseCode = "204", description = "Concílio excluído")
    @ApiResponse(responseCode = "404", description = "Concílio não encontrado")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("concílio", id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Busca concílios pelo nome", description = "Ex.: /concilios/busca?nome=vaticano")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping("/busca")
    public PagedModel<EntityModel<Concilio>> buscarPorNome(@RequestParam String nome,
                                                           @ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findByNomeContainingIgnoreCase(nome, pageable), this::toModel);
    }

    private List<Papa> buscarPapas(List<Papa> papasInformados) {
        List<Papa> papas = new ArrayList<>();
        for (Papa papa : papasInformados) {
            papas.add(papaRepository.findById(papa.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("papa", papa.getId())));
        }
        return papas;
    }

    private EntityModel<Concilio> toModel(Concilio concilio) {
        return EntityModel.of(concilio,
                linkTo(methodOn(ConcilioController.class).buscar(concilio.getId())).withSelfRel(),
                linkTo(methodOn(ConcilioController.class).atualizar(concilio.getId(), null)).withRel("update"),
                linkTo(methodOn(ConcilioController.class).excluir(concilio.getId())).withRel("delete"),
                linkTo(ConcilioController.class).withRel("concilios"));
    }
}
