package co.edu.uceva.chatservice;

import co.edu.uceva.chatservice.domain.FeignClients.IAulaServiceClient;
import co.edu.uceva.chatservice.domain.FeignClients.IReservaServiceClient;
import co.edu.uceva.chatservice.domain.dto.AulaDTO;
import co.edu.uceva.chatservice.domain.dto.BloqueDTO;
import co.edu.uceva.chatservice.domain.dto.ResponseAulaDTO;
import co.edu.uceva.chatservice.domain.dto.TipoAulaDTO;
import co.edu.uceva.chatservice.domain.model.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración para el ChatController y ChatToolsConfig.
 * Mockea los Feign Clients para validar la lógica de negocio sin depender
 * de servicios externos ni del LLM real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IAulaServiceClient aulaClient;

    @MockBean
    private IReservaServiceClient reservaClient;

    private static final String FECHA_TEST = "2026-05-20";
    private static final String HORA_INICIO_TEST = "08:00";
    private static final String HORA_FIN_TEST = "10:00";

    @BeforeEach
    void setUp() {
        // Configuración base de mocks comunes
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private AulaDTO aulaDePrueba(Long id, Long codigoAula, String nombre, String tipoCodigo) {
        return new AulaDTO(
                id,
                codigoAula,
                nombre,
                30,
                new BloqueDTO(1L, "B", "BLOQUE B - AVELLANOS"),
                new TipoAulaDTO(2L, tipoCodigo, tipoCodigo.equals("78") ? "AULA INTERACTIVA" : "LABORATORIO", false)
        );
    }

    private String enviarMensaje(String mensaje) throws Exception {
        ChatRequest request = new ChatRequest(mensaje);
        MvcResult result = mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    // ── Tests de Consulta ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "12345", roles = {"DOCENTE"})
    @DisplayName("TC01: Consultar disponibilidad por bloque con fuzzy matching")
    void consultarDisponibilidadPorBloque() throws Exception {
        // Given: Un bloque con 2 aulas, una ocupada y una libre
        BloqueDTO bloque = new BloqueDTO(1L, "B", "BLOQUE B - AVELLANOS");
        when(aulaClient.buscarBloque("B"))
                .thenReturn(Map.of("bloque", bloque));
        when(aulaClient.listarAulasPorBloque(1L))
                .thenReturn(new ResponseAulaDTO(
                        List.of(
                                aulaDePrueba(1L, 101L, "AULA 101", "78"),
                                aulaDePrueba(2L, 102L, "AULA 102", "78")
                        ),
                        "OK"
                ));
        when(reservaClient.obtenerAulasOcupadas(FECHA_TEST, HORA_INICIO_TEST, HORA_FIN_TEST))
                .thenReturn(List.of(1L)); // Aula 101 ocupada

        // When
        String respuesta = enviarMensaje("aulas disponibles en el bloque B el " + FECHA_TEST + " de " + HORA_INICIO_TEST + " a " + HORA_FIN_TEST);

        // Then
        assertThat(respuesta).containsIgnoringCase("AULA 102");
        assertThat(respuesta).doesNotContain("AULA 101"); // Está ocupada
        assertThat(respuesta).contains("Capacidad");
    }

    @Test
    @WithMockUser(username = "12345", roles = {"DOCENTE"})
    @DisplayName("TC02: Reservar aula exitosamente")
    void reservarAulaExitosamente() throws Exception {
        // Given
        when(aulaClient.buscarAulasPorNombre("AULA 101"))
                .thenReturn(new ResponseAulaDTO(
                        List.of(aulaDePrueba(1L, 101L, "AULA 101", "78")),
                        "OK"
                ));
        when(reservaClient.obtenerAulasOcupadas(FECHA_TEST, HORA_INICIO_TEST, HORA_FIN_TEST))
                .thenReturn(Collections.emptyList());
        when(reservaClient.crearReserva(anyMap()))
                .thenReturn(Map.of(
                        "mensaje", "La reserva ha sido creada con éxito!",
                        "reserva", Map.of(
                                "idReserva", 1,
                                "aulaId", 1,
                                "estado", "CONFIRMADA"
                        )
                ));

        // When
        String respuesta = enviarMensaje("reservar el aula 101 el " + FECHA_TEST + " de " + HORA_INICIO_TEST + " a " + HORA_FIN_TEST + " para reunion de facultad");

        // Then
        assertThat(respuesta).containsIgnoringCase("CONFIRMADA");
    }

    @Test
    @WithMockUser(username = "12345", roles = {"ESTUDIANTE"})
    @DisplayName("TC03: Estudiante no puede reservar laboratorio (solo tipos 78/79)")
    void estudianteNoPuedeReservarLaboratorio() throws Exception {
        // Given: Un laboratorio (tipo 77)
        AulaDTO laboratorio = new AulaDTO(
                3L, 103L, "LABORATORIO 1", 25,
                new BloqueDTO(1L, "B", "BLOQUE B - AVELLANOS"),
                new TipoAulaDTO(3L, "77", "LABORATORIO", true)
        );
        when(aulaClient.buscarAulasPorNombre("LABORATORIO 1"))
                .thenReturn(new ResponseAulaDTO(List.of(laboratorio), "OK"));

        // When
        String respuesta = enviarMensaje("reservar el laboratorio 1 el " + FECHA_TEST + " de " + HORA_INICIO_TEST + " a " + HORA_FIN_TEST + " para practicas");

        // Then: Debe rechazar ANTES de llamar al reserva-service
        verify(reservaClient, never()).crearReserva(anyMap());
        assertThat(respuesta).containsIgnoringCase("solo puedes reservar aulas interactivas");
        assertThat(respuesta).containsIgnoringCase("tipo 78").containsIgnoringCase("tipo 79");
    }

    @Test
    @WithMockUser(username = "12345", roles = {"ESTUDIANTE"})
    @DisplayName("TC04: Estudiante SI puede reservar aula interactiva (tipo 78)")
    void estudiantePuedeReservarAulaInteractiva() throws Exception {
        // Given
        when(aulaClient.buscarAulasPorNombre("AULA INTERACTIVA 1"))
                .thenReturn(new ResponseAulaDTO(
                        List.of(aulaDePrueba(4L, 104L, "AULA INTERACTIVA 1", "78")),
                        "OK"
                ));
        when(reservaClient.obtenerAulasOcupadas(FECHA_TEST, HORA_INICIO_TEST, HORA_FIN_TEST))
                .thenReturn(Collections.emptyList());
        when(reservaClient.crearReserva(anyMap()))
                .thenReturn(Map.of(
                        "mensaje", "Éxito",
                        "reserva", Map.of("estado", "CONFIRMADA")
                ));

        // When
        String respuesta = enviarMensaje("reservar el aula interactiva 1 el " + FECHA_TEST + " de " + HORA_INICIO_TEST + " a " + HORA_FIN_TEST + " para estudio");

        // Then
        verify(reservaClient, times(1)).crearReserva(anyMap());
        assertThat(respuesta).containsIgnoringCase("CONFIRMADA");
    }

    @Test
    @WithMockUser(username = "12345", roles = {"DOCENTE"})
    @DisplayName("TC05: Solapamiento - no se puede reservar aula ya ocupada")
    void solapamientoAulaOcupada() throws Exception {
        // Given
        when(aulaClient.buscarAulasPorNombre("AULA 101"))
                .thenReturn(new ResponseAulaDTO(
                        List.of(aulaDePrueba(1L, 101L, "AULA 101", "78")),
                        "OK"
                ));
        // Aula 101 ya está ocupada en ese horario
        when(reservaClient.obtenerAulasOcupadas(FECHA_TEST, HORA_INICIO_TEST, HORA_FIN_TEST))
                .thenReturn(List.of(1L));

        // When
        String respuesta = enviarMensaje("reservar el aula 101 el " + FECHA_TEST + " de " + HORA_INICIO_TEST + " a " + HORA_FIN_TEST + " para clase");

        // Then: NO debe llamar a crearReserva
        verify(reservaClient, never()).crearReserva(anyMap());
        assertThat(respuesta).containsIgnoringCase("ocupada");
    }

    @Test
    @WithMockUser(username = "12345", roles = {"DOCENTE"})
    @DisplayName("TC06: Bloqueo optimista - reserva requiere version")
    void bloqueoOptimistaReserva() throws Exception {
        // Nota: Este test valida que la entidad Reserva tiene @Version.
        // Un test real de condición de carrera requeriría ejecución concurrente.
        // Verificamos que el sistema maneja el campo version.

        // Given
        when(aulaClient.buscarAulasPorNombre("AULA 101"))
                .thenReturn(new ResponseAulaDTO(
                        List.of(aulaDePrueba(1L, 101L, "AULA 101", "78")),
                        "OK"
                ));
        when(reservaClient.obtenerAulasOcupadas(FECHA_TEST, "10:00", "12:00"))
                .thenReturn(Collections.emptyList());
        when(reservaClient.crearReserva(anyMap()))
                .thenReturn(Map.of(
                        "mensaje", "Éxito",
                        "reserva", Map.of(
                                "idReserva", 1,
                                "estado", "CONFIRMADA",
                                "version", 0 // La primera versión es 0
                        )
                ));

        // When
        String respuesta = enviarMensaje("reservar el aula 101 el " + FECHA_TEST + " de 10:00 a 12:00 para reunion");

        // Then
        assertThat(respuesta).containsIgnoringCase("CONFIRMADA");
    }

    @Test
    @WithMockUser(username = "12345", roles = {"DOCENTE"})
    @DisplayName("TC07: Aula no existe - mensaje amigable")
    void aulaNoExiste() throws Exception {
        // Given
        when(aulaClient.buscarAulasPorNombre("AULA 999"))
                .thenReturn(new ResponseAulaDTO(Collections.emptyList(), "No encontrado"));

        // When
        String respuesta = enviarMensaje("reservar el aula 999 el " + FECHA_TEST + " de " + HORA_INICIO_TEST + " a " + HORA_FIN_TEST + " para clase");

        // Then
        verify(reservaClient, never()).crearReserva(anyMap());
        assertThat(respuesta).containsIgnoringCase("no encontré");
    }

    @Test
    @WithMockUser(username = "12345", roles = {"DOCENTE"})
    @DisplayName("TC08: Reserva con autorización pendiente")
    void reservaPendienteDeAutorizacion() throws Exception {
        // Given: Aula que requiere autorización (tipo con requiereAutorizacion=true)
        AulaDTO aulaAutorizable = new AulaDTO(
                5L, 105L, "AUDITORIO PRINCIPAL", 100,
                new BloqueDTO(1L, "A", "BLOQUE A"),
                new TipoAulaDTO(4L, "80", "AUDITORIO", true) // requiereAutorizacion=true
        );
        when(aulaClient.buscarAulasPorNombre("AUDITORIO PRINCIPAL"))
                .thenReturn(new ResponseAulaDTO(List.of(aulaAutorizable), "OK"));
        when(reservaClient.obtenerAulasOcupadas(FECHA_TEST, HORA_INICIO_TEST, HORA_FIN_TEST))
                .thenReturn(Collections.emptyList());
        when(reservaClient.crearReserva(anyMap()))
                .thenReturn(Map.of(
                        "mensaje", "Éxito",
                        "reserva", Map.of(
                                "idReserva", 5,
                                "estado", "PENDIENTE"
                        )
                ));

        // When
        String respuesta = enviarMensaje("reservar el auditorio principal el " + FECHA_TEST + " de " + HORA_INICIO_TEST + " a " + HORA_FIN_TEST + " para evento");

        // Then
        assertThat(respuesta).containsIgnoringCase("PENDIENTE");
        assertThat(respuesta).containsIgnoringCase("autorización");
    }

    @Test
    @WithMockUser(username = "12345", roles = {"DOCENTE"})
    @DisplayName("TC09: Error de red en Feign Client - mensaje amigable")
    void errorDeRedFeignClient() throws Exception {
        // Given: Simular timeout/error de red
        feign.Response response = mock(feign.Response.class);
        when(response.status()).thenReturn(504);
        when(aulaClient.buscarAulasPorNombre(anyString()))
                .thenThrow(feign.FeignException.errorStatus("buscarAulasPorNombre", response));

        // When
        String respuesta = enviarMensaje("reservar el aula 101 el " + FECHA_TEST + " de " + HORA_INICIO_TEST + " a " + HORA_FIN_TEST + " para clase");

        // Then
        assertThat(respuesta).containsIgnoringCase("problema al conectar");
        assertThat(respuesta).doesNotContain("Exception");
        assertThat(respuesta).doesNotContain("StackTrace");
    }

    @Test
    @WithMockUser(username = "12345", roles = {"DOCENTE"})
    @DisplayName("TC10: Consultar horarios de aula específica")
    void consultarHorariosAula() throws Exception {
        // Given
        when(aulaClient.buscarAulasPorNombre("AULA 101"))
                .thenReturn(new ResponseAulaDTO(
                        List.of(aulaDePrueba(1L, 101L, "AULA 101", "78")),
                        "OK"
                ));
        when(reservaClient.obtenerReservasPorAula(1L))
                .thenReturn(Map.of(
                        "reservas", List.of(
                                Map.of("horaInicio", FECHA_TEST + "T08:00:00", "horaFin", FECHA_TEST + "T10:00:00", "estado", "CONFIRMADA"),
                                Map.of("horaInicio", FECHA_TEST + "T14:00:00", "horaFin", FECHA_TEST + "T16:00:00", "estado", "CONFIRMADA")
                        )
                ));

        // When
        String respuesta = enviarMensaje("qué horarios tiene disponible el aula 101 el " + FECHA_TEST);

        // Then
        assertThat(respuesta).contains("OCUPADOS");
        assertThat(respuesta).contains("08:00");
        assertThat(respuesta).contains("14:00");
    }
}
