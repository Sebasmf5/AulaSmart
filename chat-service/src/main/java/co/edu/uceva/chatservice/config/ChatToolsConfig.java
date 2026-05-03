package co.edu.uceva.chatservice.config;

import co.edu.uceva.chatservice.domain.model.ConsultaDisponibilidadArgs;
import co.edu.uceva.chatservice.domain.model.ReservarAulaArgs;
import co.edu.uceva.chatservice.domain.model.ReservarAulaArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Function;

@Configuration
public class ChatToolsConfig {

    @Bean
    @Description("Úsalo SIEMPRE que el usuario quiera consultar, preguntar o saber qué aulas están disponibles o libres en un horario determinado.")
    public Function<ConsultaDisponibilidadArgs, String> consultarDisponibilidadTool() {
        return (args) -> {
            // AQUÍ VA TU LÓGICA DE NEGOCIO REAL
            System.out.println("El LLM quiere consultar disponibilidad.");
            System.out.println("Aula (Opcional): " + args.aulaId());
            System.out.println("Fecha: " + args.fecha());
            System.out.println("Desde: " + args.horaInicio() + " Hasta: " + args.horaFin());

            // Simulamos la respuesta que enviaríamos de vuelta al LLM
            if (args.aulaId() != null) {
                return "Para los datos dados, el aula " + args.aulaId() + " SÍ está disponible en ese horario.";
            } else {
                return "Para ese horario están disponibles las aulas: B103, C201 y el auditorio principal.";
            }
        };
    }

    @Bean
    @Description("Úsalo SIEMPRE que el usuario dé una orden directa de separar, agendar o reservar un aula específica.")
    public Function<ReservarAulaArgs, String> reservarAulaTool() {
        return (args) -> {
            System.out.println("=== 1. DATOS EXTRAÍDOS POR LA IA ===");
            System.out.println("Aula a reservar: " + args.aulaId());
            System.out.println("Fecha y horas: " + args.fecha() + " de " + args.horaInicio() + " a " + args.horaFin());
            
            System.out.println("=== 2. DATOS EXTRAÍDOS DE SPRING SECURITY (JWT) ===");
            // Obtenemos el contexto de seguridad del hilo que está haciendo la petición
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            String idSolicitante = "DESCONOCIDO";
            String roles = "SIN_ROL";

            // Si el usuario está autenticado con JWT, extraemos sus datos de forma 100% segura
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                idSolicitante = auth.getName(); // Puede ser el ID o el username que viaja en el JWT
                roles = auth.getAuthorities().toString(); // Ej. [ROLE_DOCENTE]
            } else {
                // Mock para pruebas si aún no pasas el Token en Postman
                idSolicitante = "DOCENTE_MOCK_999";
                roles = "[ROLE_DOCENTE]";
            }

            System.out.println("ID Usuario: " + idSolicitante);
            System.out.println("Roles: " + roles);

            System.out.println("=== 3. LÓGICA DE NEGOCIO ===");
            // Aquí es donde construirías el ReservaDTO que vimos en tu reserva-service:
            // ReservaDTO reserva = new ReservaDTO();
            // reserva.setCodigoAula(Long.valueOf(args.aulaId()));
            // reserva.setIdSolicitante(Long.valueOf(idSolicitante)); // Dato SEGURO, no viene del prompt
            // reserva.setOrigen("AULASMART_CHAT");
            
            // Llama a tu microservicio: reservaClient.crearReserva(reserva);

            return "Éxito. La reserva para el aula " + args.aulaId() + " se ha realizado correctamente a nombre del usuario ID: " + idSolicitante;
        };
    }
}
