package senac.tsi.apipapas.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import senac.tsi.apipapas.model.Plano;
import senac.tsi.apipapas.seguranca.ApiKeyFilter;
import senac.tsi.apipapas.seguranca.Planos;

import java.math.BigDecimal;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(ApiKeyFilter.PLANOS)
@Tag(name = "Planos", description = "Preço e limites de cada plano. A consulta é pública e não gasta a cota mensal")
public class PlanoController {

    private final Planos planos;

    PlanoController(Planos planos) {
        this.planos = planos;
    }

    @Relation(collectionRelation = "planos")
    public record PlanoDisponivel(
            @Schema(example = "BASICO") Plano plano,
            @Schema(example = "Básico") String nome,
            @Schema(description = "Preço por mês, em reais", example = "19.90") BigDecimal precoMensal,
            @Schema(description = "Requisições por minuto", example = "120") int requisicoesPorMinuto,
            @Schema(description = "Requisições por mês (renova no dia 1º)", example = "50000") long cotaMensal) {
    }

    @Operation(summary = "Lista os planos, com preço e limites",
            description = "Sem chave de API, o limite é por endereço IP e só permite consultas (GET). "
                    + "Para trocar de plano, fale com o administrador (seção 12 dos Termos de Uso).")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Planos disponíveis"))
    @GetMapping
    public CollectionModel<EntityModel<PlanoDisponivel>> listar() {
        var itens = planos.todos().stream()
                .map(l -> EntityModel.of(new PlanoDisponivel(l.plano(), l.plano().getNome(), l.precoMensal(),
                        l.porMinuto(), l.cotaMensal())))
                .toList();
        return CollectionModel.of(itens, linkTo(methodOn(PlanoController.class).listar()).withSelfRel());
    }
}
