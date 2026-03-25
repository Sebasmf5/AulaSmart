package co.edu.uceva.reservaservice;

import co.edu.uceva.reservaservice.domain.model.EstadosReserva;
import co.edu.uceva.reservaservice.domain.model.Reserva;
import co.edu.uceva.reservaservice.domain.model.RolUsuario;
import co.edu.uceva.reservaservice.domain.service.IReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class ReservaConcurrencyTest {

	@Autowired
	private IReservaService reservaService;

	@Test
	void testReservasConcurrentesMismoHorario() throws InterruptedException {

		int numeroUsuarios = 2;

		ExecutorService executor =
				Executors.newFixedThreadPool(numeroUsuarios);

		CountDownLatch latch =
				new CountDownLatch(numeroUsuarios);

		Runnable tarea = () -> {

			try {

				Reserva reserva = new Reserva();

				reserva.setCodigoAula(1L);

				reserva.setHoraInicio(
						LocalDateTime.of(2026,4,10,10,0)
				);

				reserva.setHoraFin(
						LocalDateTime.of(2026,4,10,12,0)
				);

				reserva.setIdSolicitante(1L);

				reserva.setGrupo("Sistemas");

				reserva.setRolSolicitante(RolUsuario.DOCENTE);

				reserva.setEstado(EstadosReserva.PENDIENTE);

				reservaService.addReserva(reserva);

				System.out.println("Reserva creada");

			} catch (Exception e) {

				e.printStackTrace(); // ← importante agregar esto

				System.out.println("Conflicto detectado");

			} finally {

				latch.countDown();

			}

		};

		executor.submit(tarea);
		executor.submit(tarea);

		latch.await();

	}
}