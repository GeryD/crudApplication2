package com.example.crud2.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue
    @NonNull
    private Long id;

    @Column(nullable = false)
    @NonNull
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    @NonNull
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;
}
