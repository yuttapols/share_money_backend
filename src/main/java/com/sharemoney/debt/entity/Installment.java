package com.sharemoney.debt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "installments")
@Getter
@Setter
@NoArgsConstructor
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id", nullable = false)
    private Debt debt;

    @Column(nullable = false)
    private int no;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstallmentKind kind = InstallmentKind.PRINCIPAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.UNPAID;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "pay_date")
    private LocalDate payDate;

    @Column(name = "paid_at")
    private Instant paidAt;
}
