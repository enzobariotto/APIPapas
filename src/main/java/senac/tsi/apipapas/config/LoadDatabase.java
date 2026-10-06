package senac.tsi.apipapas.config;

import senac.tsi.apipapas.model.Concilio;
import senac.tsi.apipapas.model.Conclave;
import senac.tsi.apipapas.model.Enciclica;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.model.Santo;
import senac.tsi.apipapas.model.SituacaoPontificado;
import senac.tsi.apipapas.repository.ConcilioRepository;
import senac.tsi.apipapas.repository.ConclaveRepository;
import senac.tsi.apipapas.repository.EnciclicaRepository;
import senac.tsi.apipapas.repository.PapaRepository;
import senac.tsi.apipapas.repository.SantoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
class LoadDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(PapaRepository papas, ConclaveRepository conclaves,
                                   EnciclicaRepository enciclicas, ConcilioRepository concilios,
                                   SantoRepository santos) {
        return args -> {
            Papa pioIX = papas.save(new Papa("Pio IX", "Giovanni Maria Mastai-Ferretti", 255, "Itália",
                    LocalDate.of(1846, 6, 16), LocalDate.of(1878, 2, 7), SituacaoPontificado.FALECIMENTO));
            Papa joaoXXIII = papas.save(new Papa("João XXIII", "Angelo Giuseppe Roncalli", 261, "Itália",
                    LocalDate.of(1958, 10, 28), LocalDate.of(1963, 6, 3), SituacaoPontificado.FALECIMENTO));
            Papa pauloVI = papas.save(new Papa("Paulo VI", "Giovanni Battista Montini", 262, "Itália",
                    LocalDate.of(1963, 6, 21), LocalDate.of(1978, 8, 6), SituacaoPontificado.FALECIMENTO));
            Papa joaoPauloII = papas.save(new Papa("João Paulo II", "Karol Józef Wojtyła", 264, "Polônia",
                    LocalDate.of(1978, 10, 16), LocalDate.of(2005, 4, 2), SituacaoPontificado.FALECIMENTO));
            Papa bentoXVI = papas.save(new Papa("Bento XVI", "Joseph Aloisius Ratzinger", 265, "Alemanha",
                    LocalDate.of(2005, 4, 19), LocalDate.of(2013, 2, 28), SituacaoPontificado.RENUNCIA));
            Papa francisco = papas.save(new Papa("Francisco", "Jorge Mario Bergoglio", 266, "Argentina",
                    LocalDate.of(2013, 3, 13), LocalDate.of(2025, 4, 21), SituacaoPontificado.FALECIMENTO));
            Papa leaoXIV = papas.save(new Papa("Leão XIV", "Robert Francis Prevost", 267, "Estados Unidos",
                    LocalDate.of(2025, 5, 8), null, SituacaoPontificado.EM_EXERCICIO));

            conclaves.save(new Conclave(LocalDate.of(1978, 10, 14), LocalDate.of(1978, 10, 16), 111, 8, joaoPauloII));
            conclaves.save(new Conclave(LocalDate.of(2005, 4, 18), LocalDate.of(2005, 4, 19), 115, 4, bentoXVI));
            conclaves.save(new Conclave(LocalDate.of(2013, 3, 12), LocalDate.of(2013, 3, 13), 115, 5, francisco));
            conclaves.save(new Conclave(LocalDate.of(2025, 5, 7), LocalDate.of(2025, 5, 8), 133, 4, leaoXIV));

            enciclicas.save(new Enciclica("Pacem in Terris", "Paz na Terra", LocalDate.of(1963, 4, 11), joaoXXIII));
            enciclicas.save(new Enciclica("Populorum Progressio", "O Desenvolvimento dos Povos", LocalDate.of(1967, 3, 26), pauloVI));
            enciclicas.save(new Enciclica("Fides et Ratio", "Fé e Razão", LocalDate.of(1998, 9, 14), joaoPauloII));
            enciclicas.save(new Enciclica("Deus Caritas Est", "Deus é Amor", LocalDate.of(2005, 12, 25), bentoXVI));
            enciclicas.save(new Enciclica("Laudato Si'", "Louvado Sejas", LocalDate.of(2015, 5, 24), francisco));
            enciclicas.save(new Enciclica("Fratelli Tutti", "Todos Irmãos", LocalDate.of(2020, 10, 3), francisco));

            concilios.save(new Concilio("Concílio Vaticano I", "Basílica de São Pedro, Vaticano",
                    LocalDate.of(1869, 12, 8), LocalDate.of(1870, 10, 20), new ArrayList<>(List.of(pioIX))));
            concilios.save(new Concilio("Concílio Vaticano II", "Basílica de São Pedro, Vaticano",
                    LocalDate.of(1962, 10, 11), LocalDate.of(1965, 12, 8), new ArrayList<>(List.of(joaoXXIII, pauloVI))));

            santos.save(new Santo("Santo Antônio de Sant'Anna Galvão (Frei Galvão)", "Brasil", LocalDate.of(2007, 5, 11), bentoXVI));
            santos.save(new Santo("São João Paulo II", "Polônia", LocalDate.of(2014, 4, 27), francisco));
            santos.save(new Santo("Santa Teresa de Calcutá", "Macedônia do Norte", LocalDate.of(2016, 9, 4), francisco));
            santos.save(new Santo("Santa Dulce dos Pobres", "Brasil", LocalDate.of(2019, 10, 13), francisco));

            log.info("Banco carregado com {} papas", papas.count());
        };
    }
}
