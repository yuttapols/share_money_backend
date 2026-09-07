package com.sharemoney.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
public class Setting {

    @Id
    @Column(length = 100)
    private String key;

    @Column(nullable = false, length = 500)
    private String value;
}
