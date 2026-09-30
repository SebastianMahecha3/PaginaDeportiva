package Fifan.t.Egoisu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Monto que escribe el usuario. Es lo único que se toma del cliente para calcular una apuesta. */
@Getter @Setter
public class ApuestaFormDto {

    @NotNull(message = "Escribe cuánto quieres apostar")
    @DecimalMin(value = "0.01", message = "La cantidad apostada debe ser mayor que 0")
    @Digits(integer = 10, fraction = 2, message = "Usa máximo 10 enteros y 2 decimales")
    private BigDecimal monto;
}
