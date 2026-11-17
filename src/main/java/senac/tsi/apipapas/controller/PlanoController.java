package senac.tsi.apipapas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import senac.tsi.apipapas.model.ChaveApi;
import senac.tsi.apipapas.model.Plano;
import senac.tsi.apipapas.repository.ChaveApiRepository;
import senac.tsi.apipapas.seguranca.ApiKeyFilter;
import senac.tsi.apipapas.seguranca.Planos;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(ApiKeyFilter.PLANOS)
@Tag(name = "Planos", description = "Preços e limites de cada plano, e o consumo da sua chave de API no mês. "
        + "Estas consultas não gastam a cota mensal")
public class PlanoController {

    private final Planos planos;
    private final ChaveApiRepository repository;

    PlanoController(Planos planos, ChaveApiRepository repository) {
        this.planos = planos;
        this.repository = repository;
    }

    @Relation(collectionRelation = "planos")
    public record PlanoDisponivel(
            @Schema(example = "BASICO") Plano plano,
            @Schema(example = "Básico") String nome,
            @Schema(description = "Preço por mês, em reais", example = "19.90") BigDecimal precoMensal,
            @Schema(description = "Requisições por minuto", example = "120") int requisicoesPorMinuto,
            @Schema(description = "Requisições por mês (renova no dia 1º)", example = "50000") long cotaMensal) {
    }

    public record UsoDaChave(
            @Schema(example = "Professor") String nome,
            @Schema(example = "papas_DhE6Z") String prefixo,
            @Schema(example = "BASICO") Plano plano,
            @Schema(description = "Requisições por minuto", example = "120") int requisicoesPorMinuto,
            @Schema(description = "Requisições por mês", example = "50000") long cotaMensal,
            @Schema(description = "Requisições feitas no mês atual", example = "1234") long usadasNoMes,
            @Schema(description = "Requisições que ainda restam no mês", example = "48766") long restantesNoMes,
            @Schema(description = "Dia em que a cota volta a zero", example = "2026-12-01") LocalDate renovaEm) {
    }

    @Operation(summary = "Lista os planos, com preço e limites",
            description = "Sem chave de API, o limite é por endereço IP e só permite consultas (GET). "
                    + "Para trocar de plano, fale com o administrador (seção 3 dos Termos de Uso).")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Planos disponíveis"))
    @GetMapping
    public CollectionModel<EntityModel<PlanoDisponivel>> listar() {
        var itens = planos.todos().stream()
                .map(l -> EntityModel.of(new PlanoDisponivel(l.plano(), l.plano().getNome(), l.precoMensal(),
                        l.porMinuto(), l.cotaMensal())))
                .toList();
        return CollectionModel.of(itens,
                linkTo(methodOn(PlanoController.class).listar()).withSelfRel(),
                linkTo(methodOn(PlanoController.class).uso(null)).withRel("uso"));
    }

    @Operation(summary = "Mostra o plano e o consumo da sua chave de API no mês",
            description = "Envie a sua chave de usuário no header X-API-Key.")
    @SecurityRequirement(name = "ApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano e consumo da chave"),
            @ApiResponse(responseCode = "401", description = "Header X-API-Key ausente, ou chave inválida ou revogada", content = @Content)
    })
    @GetMapping("/uso")
    public EntityModel<UsoDaChave> uso(HttpServletRequest request) {
        Long id = request == null ? null : (Long) request.getAttribute(ApiKeyFilter.ATRIBUTO_CHAVE_ID);
        ChaveApi chave = repository.findById(id).orElseThrow();
        Planos.Limites limites = planos.de(chave.getPlano());
        long usadas = chave.getRequisicoesNoMes();
        UsoDaChave uso = new UsoDaChave(chave.getNome(), chave.getPrefixo(), chave.getPlano(), limites.porMinuto(),
                limites.cotaMensal(), usadas, Math.max(0, limites.cotaMensal() - usadas), Planos.renovacao());
        return EntityModel.of(uso,
                linkTo(methodOn(PlanoController.class).uso(null)).withSelfRel(),
                linkTo(methodOn(PlanoController.class).listar()).withRel("planos"));
    }
}
