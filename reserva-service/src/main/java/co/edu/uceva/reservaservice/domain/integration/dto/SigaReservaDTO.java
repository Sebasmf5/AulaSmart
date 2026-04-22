package co.edu.uceva.reservaservice.domain.integration.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SigaReservaDTO {
    private String id_unico;
    private String nombre_docente_responsable;
    private Long codigo_docente_responsable;
    private Long evt_id;
    private Long cal_id;
    private String evt_title;
    private String start_dt; // Formato: "2026-04-20 07:20:00"
    private String end_dt;   // Formato: "2026-04-20 10:40:00"
    private String date_start;
    private String date_end;
    private String time_start;
    private String time_end;
    private String description;
    private String location;
    private Integer code_location; // ID del aula en SIGA
}
