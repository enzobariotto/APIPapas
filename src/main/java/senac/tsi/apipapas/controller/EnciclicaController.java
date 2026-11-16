package senac.tsi.apipapas.controller;

import senac.tsi.apipapas.exception.RecursoNaoEncontradoException;
import senac.tsi.apipapas.model.Enciclica;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.repository.EnciclicaRepository;
import senac.tsi.apipapas.repository.PapaRepository;
import senac.tsi.apipapas.validacao.RegrasHistoricas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/enciclicas")
@Tag(name = "Encíclicas", description = "Encíclicas (cartas) escritas pelos papas. Relação One-to-Many: um papa escreve várias encíclicas")
public class EnciclicaController {

    private final EnciclicaRepository repository;
    private final PapaRepository papaRepository;
    private final PagedResourcesAssembler<Enciclica> pagedAssembler;
    private final RegrasHistoricas regras;

    EnciclicaController(EnciclicaRepository repository, PapaRepository papaRepository,
                        PagedResourcesAssembler<Enciclica> pagedAssembler, RegrasHistoricas regras) {
        this.repository = repository;
        this.papaRepository = papaRepository;
        this.pagedAssembler = pagedAssembler;
        this.regras = regras;
    }

    @Operation(summary = "Lista as encíclicas",
            description = "Cada encíclica traz os dados do papa que a escreveu e o link `papa` para o registro completo dele.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de encíclicas")
    })
    @GetMapping
    public PagedModel<EntityModel<Enciclica>> listar(@ParameterObject Pageable pageable) {
        return pagedAssembler.toModel(repository.findAll(pageable), this::toModel);
    }

    @Operation(summary = "Busca uma encíclica pelo id",
            description = "Traz os links para alterar (`update`), excluir (`delete`) e para o papa (`papa`).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encíclica encontrada"),
            @ApiResponse(responseCode = "404", description = "Encíclica não encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public EntityModel<Enciclica> buscar(@Parameter(description = "Id do(a) encíclica", example = "1") @PathVariable Long id) {
        return toModel(buscarEnciclica(id));
    }

    @Operation(summary = "Cadastra uma encíclica",
            description = "O título em latim, a data de publicação e o papa são obrigatórios; o título em português é opcional. Informe o papa pelo id, por exemplo `\"papa\": {\"id\": 6}`. A data de publicação precisa estar dentro do pontificado do papa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Encíclica cadastrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Papa não encontrado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EntityModel<Enciclica>> criar(@Valid @RequestBody Enciclica novaEnciclica) {
        novaEnciclica.setId(null);
        novaEnciclica.setPapa(buscarPapa(novaEnciclica.getPapa().getId()));
        regras.validarDentroDoPontificado(novaEnciclica.getPapa(), novaEnciclica.getDataPublicacao(),
                "dataPublicacao", "A data de publicação");
        Enciclica salva = repository.save(novaEnciclica);
        return ResponseEntity.created(linkTo(methodOn(EnciclicaController.class).buscar(salva.getId())).toUri())
                .body(toModel(salva));
    }

    @Operation(summary = "Atualiza uma encíclica",
            description = "Substitui todos os dados do registro (PUT): envie o objeto completo, inclusive os campos que não mudaram. Valem as mesmas validações do cadastro.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encíclica atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Encíclica ou papa não encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public EntityModel<Enciclica> atualizar(@Parameter(description = "Id do(a) encíclica", example = "1") @PathVariable Long id, @Valid @RequestBody Enciclica dados) {
        Enciclica enciclica = buscarEnciclica(id);
        enciclica.setTitulo(dados.getTitulo());
        enciclica.setTituloPortugues(dados.getTituloPortugues());
        enciclica.setDataPublicacao(dados.getDataPublicacao());
        enciclica.setPapa(buscarPapa(dados.getPapa().getId()));
        regras.validarDentroDoPontificado(enciclica.getPapa(), enciclica.getDataPublicacao(),
                "dataPublicacao", "A data de publicação");
        return toModel(repository.save(enciclica));
    }

    @Operation(summary = "Exclui uma encíclica",
            description = "Remove a encíclica. O papa não é afetado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Encíclica excluída", content = @Content),
            @ApiResponse(responseCode = "404", description = "Encíclica não encontrada", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@Parameter(description = "Id do(a) encíclica", example = "1") @PathVariable Long id) {
        repository.delete(buscarEnciclica(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lista as encíclicas de um papa",
            description = "Consulta personalizada: encíclicas escritas pelo papa informado. É o destino do link `enciclicas` de cada papa. Ex.: /enciclicas/papa/6.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de encíclicas do papa"),
            @ApiResponse(responseCode = "404", description = "Papa não encontrado", content = @Content)
    })
    @GetMapping("/papa/{papaId}")
    public PagedModel<EntityModel<Enciclica>> buscarPorPapa(@Parameter(description = "Id do papa", example = "6") @PathVariable Long papaId,
                                                            @ParameterObject Pageable pageable) {
        if (!papaRepository.existsById(papaId)) {
            throw new RecursoNaoEncontradoException("papa", papaId);
        }
        return pagedAssembler.toModel(repository.findByPapaId(papaId, pageable), this::toModel);
    }

    private Enciclica buscarEnciclica(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("encíclica", id));
    }

    private Papa buscarPapa(Long papaId) {
        if (papaId == null) {
            throw new DadosInvalidosException("papa.id", "Informe o id do papa, ex.: {\"id\": 6}");
        }
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
