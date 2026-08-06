package com.leets.tdd.party.dto.response;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.user.domain.User;
import lombok.Getter;

@Getter
public class DeliveryPartyDetailResponse {

    private final Long id;
    private final Long creatorId;
    private final String leaderNickname;
    private final String leaderProfileImage;
    private final BigDecimal leaderMannerTemperature;
    private final String dormitory;
    private final Long foodCategoryId;
    private final String title;
    private final String description;
    private final Integer minParticipants;
    private final Integer maxParticipants;
    private final LocalDateTime orderExpectedAt;
    private final String status;
    private final LocalDateTime createdAt;

    public DeliveryPartyDetailResponse(DeliveryParty deliveryParty, User leader) {
        this.id = deliveryParty.getId();
        this.creatorId = deliveryParty.getCreatorId();
        this.leaderNickname = leader.getNickname();
        this.leaderProfileImage = leader.getProfileImageUrl();
        this.leaderMannerTemperature = leader.getMannerTemperature();
        this.dormitory = deliveryParty.getDormitory();
        this.foodCategoryId = deliveryParty.getFoodCategoryId();
        this.title = deliveryParty.getTitle();
        this.description = deliveryParty.getDescription();
        this.minParticipants = deliveryParty.getMinParticipants();
        this.maxParticipants = deliveryParty.getMaxParticipants();
        this.orderExpectedAt = deliveryParty.getOrderExpectedAt();
        this.status = deliveryParty.getStatus().name();
        this.createdAt = deliveryParty.getCreatedAt();
    }
}
