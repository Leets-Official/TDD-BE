package com.leets.tdd.party.dto.response;

import java.time.LocalDateTime;

public class CreateDeliveryPartyResponse {

    private Long id;

    private Long foodCategoryId;

    private String title;

    private String description;

    private Integer minParticipants;

    private Integer maxParticipants;

    private LocalDateTime orderExpectedAt;

    private String status;

    private LocalDateTime createdAt;
}
