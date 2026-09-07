package com.sharemoney.slip.entity;

import com.sharemoney.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;

@Entity
@Table(name = "slips")
@Getter
@Setter
@NoArgsConstructor
public class Slip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debtor_id", nullable = false)
    private User debtor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creditor_id", nullable = false)
    private User creditor;

    @Column(name = "public_id", nullable = false, length = 255)
    private String publicId;

    @Column(name = "secure_url", nullable = false, length = 500)
    private String secureUrl;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(length = 20)
    private String format;

    @Column(name = "resource_type", nullable = false, length = 20)
    private String resourceType = "image";

    @Column(nullable = false)
    private long bytes;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();
}
