package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "provinces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Province {
    @Id
    @Column(length = 20)
    String code;

    @Column(nullable = false)
    String name;

    @Column(name = "name_en")
    String nameEn;

    @Column(name = "full_name", nullable = false)
    String fullName;

    @Column(name = "full_name_en")
    String fullNameEn;

    @Column(name = "code_name")
    String codeName;
}
