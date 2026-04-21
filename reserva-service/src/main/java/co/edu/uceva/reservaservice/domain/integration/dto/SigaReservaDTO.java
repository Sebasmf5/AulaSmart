package co.edu.uceva.reservaservice.domain.integration.dto;

import lombok.Data;

@Data
public class SigaReservaDTO {
    private Long cod_resv;
    private Long cod_aula;
    private String fec_res; // Formato "DD/MM/YYYY"
    private String h_ini;   // Formato "HH:MM"
    private String h_fin;
    private String status;  // "A" (Activo), "C" (Cancelado)
    private String programa;
    private String grupo;
}
