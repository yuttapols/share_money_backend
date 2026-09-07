package com.sharemoney.debt.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PayInterestRequest(

        @NotNull
        LocalDate payDate
) {
}
