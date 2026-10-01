package com.portfoliopro.backend.dto;

import com.portfoliopro.backend.entity.Trade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeRequest {

    @NotNull
    @NotBlank
    private String ticker;

    @NotNull
    private Trade.TradeType type;

    @NotNull
    @Positive
    @Max(1_000_000)
    private Integer quantity;

    @Size(max = 36)
    @Pattern(regexp = "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    private String idempotencyKey;
}
