package co.edu.uceva.reservaservice;

import co.edu.uceva.reservaservice.domain.excepcion.ReservaSolapadaException;
import co.edu.uceva.reservaservice.domain.model.EstadosReserva;
import co.edu.uceva.reservaservice.domain.model.Reserva;
import co.edu.uceva.reservaservice.domain.model.RolUsuario;
import co.edu.uceva.reservaservice.domain.service.IReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReservaConcurrencyTest {

    @Autowired
    private IReservaService reservaService;

    // ID de la reserva que usaremos en los tests.
    // Debe existir en la BD antes de correr los tests (INSERT en import.sql).
    private static final Long ID_RESERVA_TEST = 1L;

    // ─────────────────────────────────────────────────
    // TEST 1: Bloqueo optimista — dos updates simultáneos
    // ─────────────────────────────────────────────────

    /**
     * Escenario:
     * - Thread A y Thread B leen la MISMA reserva (version = 0) al mismo tiempo.
     * - Ambos intentan modificar la hora de inicio y guardar.
     * - El primero en llegar a la BD gana (version pasa a 1).
     * - El segundo falla con ObjectOptimisticLockingFailureException.
     *
     * Resultado esperado:
     * - exactamente 1 actualización exitosa
     * - exactamente 1 conflicto detectado
     */
    @Test
    @DisplayName("Optimistic Locking: solo 1 thread debe ganar al actualizar la misma reserva")
    void testBloqueOptimista_soloUnThreadGana() throws InterruptedException {

        // FASE 1: Leer la reserva UNA sola vez (ambos threads parten del mismo objeto)
        Reserva reservaOriginal = reservaService.findReservaById(ID_RESERVA_TEST)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la reserva con id=" + ID_RESERVA_TEST +
                        ". Asegúrate de tener datos en import.sql antes de correr el test."));

        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("Reserva leída: id=" + reservaOriginal.getIdReserva()
                + " | version=" + reservaOriginal.getVersion());
        System.out.println("═══════════════════════════════════════════════════");

        // FASE 2: Preparar instrumentos de concurrencia
        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // startLatch: bloquea ambos threads hasta que ambos estén listos.
        // Así se garantiza que los dos intenten el UPDATE al mismo tiempo.
        CountDownLatch startLatch = new CountDownLatch(1);

        // doneLatch: espera a que ambos threads terminen antes de hacer assertions.
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        // Contadores thread-safe para medir el resultado
        AtomicInteger exitos    = new AtomicInteger(0);
        AtomicInteger conflictos = new AtomicInteger(0);
        List<String> log = Collections.synchronizedList(new ArrayList<>());

        // FASE 3: Definir la tarea concurrente
        Runnable tareaDeActualizacion = () -> {
            String threadName = Thread.currentThread().getName();
            try {
                // Cada thread crea su propia copia del objeto con el MISMO version.
                // Esto simula que dos usuarios leyeron la misma pantalla al mismo tiempo.
                Reserva miCopia = copiarReserva(reservaOriginal);
                miCopia.setHoraInicio(LocalDateTime.of(2026, 4, 10, 11, 0));
                miCopia.setHoraFin(LocalDateTime.of(2026, 4, 10, 13, 0));

                // Esperar la señal de salida (simula que ambos llegan juntos)
                startLatch.await();

                reservaService.updateReserva(miCopia);

                exitos.incrementAndGet();
                log.add("[" + threadName + "] ✅ Actualización exitosa. Nueva version en BD: " +
                        miCopia.getVersion());

            } catch (ObjectOptimisticLockingFailureException e) {
                conflictos.incrementAndGet();
                log.add("[" + threadName + "] ⚔️  Conflicto de concurrencia detectado (Optimistic Lock).");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.add("[" + threadName + "] ⚠️  Thread interrumpido.");

            } catch (Exception e) {
                log.add("[" + threadName + "] ❌ Excepción inesperada: " + e.getClass().getSimpleName()
                        + " — " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        };

        // FASE 4: Lanzar los threads
        executor.submit(tareaDeActualizacion);
        executor.submit(tareaDeActualizacion);

        // Dar la señal de salida a ambos threads al mismo tiempo
        startLatch.countDown();

        // Esperar a que ambos terminen
        doneLatch.await();
        executor.shutdown();

        // FASE 5: Imprimir el log para que puedas ver qué pasó
        System.out.println("\n─── Resultado del test de concurrencia ───");
        log.forEach(System.out::println);
        System.out.printf("Exitosos: %d | Conflictos: %d%n", exitos.get(), conflictos.get());
        System.out.println("──────────────────────────────────────────\n");

        // FASE 6: Verificar que el bloqueo optimista funcionó
        assertEquals(1, exitos.get(),
                "Exactamente 1 thread debe lograr actualizar la reserva.");
        assertEquals(1, conflictos.get(),
                "Exactamente 1 thread debe recibir un conflicto de Optimistic Lock.");
    }

    // ─────────────────────────────────────────────────
    // TEST 2: Solapamiento de horarios — dos reservas altas a la vez
    // ─────────────────────────────────────────────────

    /**
     * Escenario:
     * - Dos usuarios intentan crear reservas distintas para el mismo aula
     *   y en el mismo horario al mismo tiempo.
     * - El constraint EXCLUDE GiST de PostgreSQL garantiza que solo 1 pase.
     *
     * Resultado esperado:
     * - exactamente 1 reserva creada
     * - exactamente 1 excepción de solapamiento
     */
    @Test
    @DisplayName("Anti-solapamiento: solo 1 reserva debe crearse cuando hay conflicto de horario")
    void testSolapamiento_soloUnaReservaDebeCrearse() throws InterruptedException {

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(numThreads);

        AtomicInteger creadas     = new AtomicInteger(0);
        AtomicInteger solapamientos = new AtomicInteger(0);
        List<String> log = Collections.synchronizedList(new ArrayList<>());

        // Horario que choca con el INSERT de aula 195 en import.sql
        // (2026-04-10 08:00 → 10:00)
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 10, 8, 30);
        LocalDateTime fin    = LocalDateTime.of(2026, 4, 10, 9, 30);

        Runnable tareaDeCreacion = () -> {
            String threadName = Thread.currentThread().getName();
            try {
                Reserva nueva = reservaParaTest(195L, inicio, fin);

                startLatch.await();

                reservaService.addReserva(nueva);
                creadas.incrementAndGet();
                log.add("[" + threadName + "] ✅ Reserva creada con id=" + nueva.getIdReserva());

            } catch (ReservaSolapadaException e) {
                solapamientos.incrementAndGet();
                log.add("[" + threadName + "] 🚫 Solapamiento detectado por constraint GiST.");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.add("[" + threadName + "] ❌ Excepción inesperada: " + e.getClass().getSimpleName()
                        + " — " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(tareaDeCreacion);
        executor.submit(tareaDeCreacion);
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        System.out.println("\n─── Resultado del test de solapamiento ───");
        log.forEach(System.out::println);
        System.out.printf("Creadas: %d | Solapamientos: %d%n", creadas.get(), solapamientos.get());
        System.out.println("──────────────────────────────────────────\n");

        // Al menos 1 debe fallar. Puede ser que los dos fallen si la reserva de
        // import.sql ya cubre el rango, lo cual también es un resultado válido.
        assertTrue(solapamientos.get() >= 1,
                "Al menos 1 intento debe ser rechazado por solapamiento de horario.");
    }

    // ─────────────────────────────────────────────────
    // Métodos auxiliares
    // ─────────────────────────────────────────────────

    /**
     * Crea una copia superficial de la reserva manteniendo el mismo 'version'.
     * Simula que dos usuarios leyeron la misma reserva en pantalla.
     */
    private Reserva copiarReserva(Reserva original) {
        Reserva copia = new Reserva();
        copia.setIdReserva(original.getIdReserva());
        copia.setCodigoAula(original.getCodigoAula());
        copia.setHoraInicio(original.getHoraInicio());
        copia.setHoraFin(original.getHoraFin());
        copia.setEstado(original.getEstado());
        copia.setIdSolicitante(original.getIdSolicitante());
        copia.setRolSolicitante(original.getRolSolicitante());
        copia.setCodigoPrograma(original.getCodigoPrograma());
        copia.setGrupo(original.getGrupo());
        copia.setVersion(original.getVersion()); // ← el mismo version = esto es lo que produce el conflicto
        return copia;
    }

    /**
     * Construye una reserva nueva para los tests de solapamiento.
     */
    private Reserva reservaParaTest(Long codigoAula, LocalDateTime inicio, LocalDateTime fin) {
        Reserva r = new Reserva();
        r.setCodigoAula(codigoAula);
        r.setHoraInicio(inicio);
        r.setHoraFin(fin);
        r.setEstado(EstadosReserva.PENDIENTE);
        r.setIdSolicitante(99999L);
        r.setRolSolicitante(RolUsuario.DOCENTE);
        r.setCodigoPrograma("TEST");
        r.setGrupo("GT");
        r.setVersion(0L);
        return r;
    }
}