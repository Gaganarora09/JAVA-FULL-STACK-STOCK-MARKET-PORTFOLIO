package com.portfoliopro.backend.dto;

import com.portfoliopro.backend.entity.Trade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    private Integer quantity;
}
