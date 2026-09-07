package com.sharemoney.user.entity;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.entity.BaseEntity;
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

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creditor_id")
    private User creditor;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_public_id", length = 500)
    private String avatarPublicId;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "preferred_language", nullable = false, length = 5)
    private String preferredLanguage = "th";

    @Column(nullable = false)
    private boolean active = true;
}
