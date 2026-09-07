package com.sharemoney.profile.entity;

import com.sharemoney.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creditor_id", nullable = false, unique = true)
    private User creditor;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_no", nullable = false, length = 30)
    private String accountNo;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "payment_note", length = 255)
    private String paymentNote;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
