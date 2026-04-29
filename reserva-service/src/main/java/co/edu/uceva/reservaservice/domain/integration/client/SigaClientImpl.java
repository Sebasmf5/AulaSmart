package co.edu.uceva.reservaservice.domain.integration.client;

import co.edu.uceva.reservaservice.domain.integration.dto.SigaReservaDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SigaClientImpl implements ISigaClient {

    private final RestTemplate restTemplate;
    
    // Utilizamos la URL real proporcionada
    @Value("${siga.api.url:https://uceva.datasae.co/siga_new/web/app.php/publicomanejoespacios}")
    private String sigaApiUrl;

    public SigaClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<SigaReservaDTO> obtenerReservasPorAula(Long codigoAula) {
        // Para obtener "todas", consultamos desde hoy hasta 6 meses en el futuro
        String startDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = LocalDate.now().plusMonths(6).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return ejecutarConsultaSiga(codigoAula, startDate, endDate);
    }

    @Override
    public List<SigaReservaDTO> obtenerReservasPorAulaYFecha(Long codigoAula, LocalDate fecha) {
        String fechaStr = fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return ejecutarConsultaSiga(codigoAula, fechaStr, fechaStr);
    }

    private List<SigaReservaDTO> ejecutarConsultaSiga(Long codigoAula, String startDate, String endDate) {
        // Parámetro de configuración original del SIGA
        String conf = "%5B%7B%22variable%22%3A%22UTILIZA_PROGRAMACION_POR_CICLOS_O_SUB_CORTES%22%2C%22valor%22%3A%221%22%7D%5D";
        
        // Construimos la URL idéntica a la real (con un límite de 100 por si hay muchas en un día)
        String url = String.format("%s/listarEventAulaPublico?_dc=%d&calendario=calendario_publico&configuracion=%s&aula=%d&startDate=%s&endDate=%s&page=1&start=0&limit=100",
                sigaApiUrl, System.currentTimeMillis(), conf, codigoAula, startDate, endDate);
        
        try {
            // Obtenemos un JsonNode genérico para manejar la estructura de la respuesta (array o data)
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapp = new ObjectMapper();
            JsonNode root = mapp.readTree(response.getBody());
            //JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            List<SigaReservaDTO> resultList = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            
            if (root == null) return resultList;

            // Si la API devuelve un array directamente [...]
            if (root.isArray()) {
                for (JsonNode node : root) {
                    resultList.add(mapper.treeToValue(node, SigaReservaDTO.class));
                }
            } 
            // Formato real de la UCEVA: { "success": true, "evts": [...] }
            else if (root.has("evts") && root.get("evts").isArray()) {
                for (JsonNode node : root.get("evts")) {
                    resultList.add(mapper.treeToValue(node, SigaReservaDTO.class));
                }
            }
            // Si el SIGA devuelve envuelto { "data": [...] }
            else if (root.has("data") && root.get("data").isArray()) {
                for (JsonNode node : root.get("data")) {
                    resultList.add(mapper.treeToValue(node, SigaReservaDTO.class));
                }
            }
            // Otras posibles formas
            else if (root.has("items") && root.get("items").isArray()) {
                for (JsonNode node : root.get("items")) {
                    resultList.add(mapper.treeToValue(node, SigaReservaDTO.class));
                }
            }
            
            return resultList;
        } catch (Exception e) {
            System.err.println("Error consultando API del SIGA real: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
