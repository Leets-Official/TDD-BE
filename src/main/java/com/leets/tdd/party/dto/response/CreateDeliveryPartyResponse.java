package com.leets.tdd.party.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CreateDeliveryPartyResponse {

    private Long id;

    private Long foodCategoryId;

    private String dormitory;

    private String title;

    private String description;

    private Integer minParticipants;

    private Integer maxParticipants;

    private LocalDateTime orderExpectedAt;

    private String status;

    private LocalDateTime createdAt;
}
