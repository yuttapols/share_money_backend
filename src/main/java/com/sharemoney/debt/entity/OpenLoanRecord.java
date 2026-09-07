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
@Table(name = "open_loan_records")
@Getter
@Setter
@NoArgsConstructor
public class OpenLoanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_id", nullable = false)
    private Debt debt;

    @Column(nullable = false)
    private int no;

    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;

    @Column(name = "total_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal interest = BigDecimal.ZERO;

    @Column(name = "remaining_principal", nullable = false, precision = 14, scale = 2)
    private BigDecimal remainingPrincipal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.UNPAID;

    @Column(name = "paid_at")
    private Instant paidAt;
}
