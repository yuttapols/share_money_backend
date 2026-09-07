package com.sharemoney.debt.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PayInstallmentRequest(

        @NotNull
        Boolean paid,

        LocalDate payDate
) {
}
