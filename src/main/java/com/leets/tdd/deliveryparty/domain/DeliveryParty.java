package com.leets.tdd.deliveryparty.domain;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "delivery_parties")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long foodCategoryId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer minParticipants;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private LocalDateTime orderExpectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryPartyStatus status;

    @Column(nullable = false)
    private LocalDateTime closedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public DeliveryParty(
            Long foodCategoryId,
            String title,
            String description,
            Integer minParticipants,
            Integer maxParticipants,
            LocalDateTime orderExpectedAt,
            DeliveryPartyStatus status,
            LocalDateTime closedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.foodCategoryId = foodCategoryId;
        this.title = title;
        this.description = description;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.orderExpectedAt = orderExpectedAt;
        this.status = status;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
