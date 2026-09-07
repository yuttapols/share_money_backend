package com.sharemoney.debt.entity;

import com.sharemoney.common.entity.BaseEntity;
import com.sharemoney.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "debts")
@Getter
@Setter
@NoArgsConstructor
public class Debt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creditor_id", nullable = false)
    private User creditor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debtor_id", nullable = false)
    private User debtor;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DebtMethod method;

    @Column(name = "principal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "installment_count")
    private Integer installmentCount;

    @Column(name = "installment_amount", precision = 14, scale = 2)
    private BigDecimal installmentAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DebtStatus status = DebtStatus.PENDING;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @OneToMany(mappedBy = "debt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("no ASC")
    private List<Installment> installments = new ArrayList<>();

    @OneToMany(mappedBy = "debt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("no ASC")
    private List<OpenLoanRecord> openLoanRecords = new ArrayList<>();
}
