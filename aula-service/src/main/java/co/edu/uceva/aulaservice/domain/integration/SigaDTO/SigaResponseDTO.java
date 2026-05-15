package co.edu.uceva.aulaservice.domain.integration.SigaDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class SigaResponseDTO {
    private boolean success;
    private List<AulaExternaDto> data;
    private int total;
}
