package senac.tsi.apipapas.controller;

import senac.tsi.apipapas.exception.RecursoNaoEncontradoException;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.model.SituacaoPontificado;
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
@RequestMapping("/papas")
@Tag(name = "Papas", description = "Cadastro dos papas da Igreja Católica")
public class PapaController {

    private final PapaRepository repository;
    private final PagedResourcesAssembler<Papa> pagedAssembler;

    PapaController(PapaRepository repository, PagedResourcesAssembler<Papa> pagedAssembler) {
        this.repository = repository;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(summary = "Lista os papas")
    @GetMapping
    public PagedModel<EntityModel<Papa>> listar(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModel);
    }

    @Operation(summary = "Busca um papa pelo id")
    @GetMapping("/{id}")
    public EntityModel<Papa> buscar(@PathVariable Long id) {
        Papa papa = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("papa", id));
        return toModel(papa);
    }

    @Operation(summary = "Cadastra um papa")
    @PostMapping
    public ResponseEntity<EntityModel<Papa>> criar(@Valid @RequestBody Papa novoPapa) {
        Papa salvo = repository.save(novoPapa);
        EntityModel<Papa> model = toModel(salvo);
        return ResponseEntity.created(linkTo(methodOn(PapaController.class).buscar(salvo.getId())).toUri()).body(model);
    }

    @Operation(summary = "Atualiza um papa")
    @PutMapping("/{id}")
    public EntityModel<Papa> atualizar(@PathVariable Long id, @Valid @RequestBody Papa dados) {
        Papa papa = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("papa", id));
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
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException("papa", id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Busca papas pela situação")
    @GetMapping("/situacao/{situacao}")
    public PagedModel<EntityModel<Papa>> buscarPorSituacao(@PathVariable SituacaoPontificado situacao,
                                                           @ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findBySituacao(situacao, pageable), this::toModel);
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
