package com.leets.tdd.party.dto.request;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateDeliveryPartyRequest {

    private Long foodCategoryId;

    private String title;

    private String description;

    private Integer minParticipants;

    private Integer maxParticipants;

    private LocalDateTime orderExpectedAt;
}
