package co.edu.uceva.chatservice.domain.FeignClients;

import co.edu.uceva.chatservice.domain.config.FeignClientInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "reserva-service",
        url = "${conexion.servicios.reserva-service.url}",
        configuration = FeignClientInterceptor.class
)
public interface IReservaServiceClient {
    @GetMapping("/api/v1/reserva-service/reservas/aula/{aulaId}/agregadas")
    Map<String, Object> obtenerReservasPorAula(@PathVariable Long aulaId);

    @PostMapping("/api/v1/reserva-service/reservas")
    Map<String, Object> crearReserva(@RequestBody Map<String, Object> reserva);

    @GetMapping("/api/v1/reserva-service/reservas/ocupadas")
    List<Long> obtenerAulasOcupadas(
            @RequestParam("fecha") String fecha,
            @RequestParam("horaInicio") String horaInicio,
            @RequestParam("horaFin") String horaFin
    );
}
