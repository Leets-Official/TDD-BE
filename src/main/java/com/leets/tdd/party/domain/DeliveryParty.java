package com.leets.tdd.party.domain;

import com.leets.tdd.settlement.domain.SettlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "delivery_parties")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** 배달팟의 공통 정보와 정산 진행 상태를 함께 저장하는 엔티티입니다. */
public class DeliveryParty {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  @Column(nullable = false)
  private Long version;

  @Column(name = "creator_id", nullable = false)
  private Long creatorId;

  @Column(name = "food_category_id", nullable = false)
  private Long foodCategoryId;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "min_participants", nullable = false)
  private Integer minParticipants;

  @Column(name = "max_participants", nullable = false)
  private Integer maxParticipants;

  @Column(name = "order_expected_at", nullable = false)
  private LocalDateTime orderExpectedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PartyStatus status;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "settlement_status", nullable = false, length = 20)
  private SettlementStatus settlementStatus;

  @Column(name = "settlement_total_amount")
  private Integer settlementTotalAmount;

  @Column(name = "settlement_requested_at")
  private LocalDateTime settlementRequestedAt;

  @Column(name = "settlement_bank_account_id")
  private Long settlementBankAccountId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public DeliveryParty(
          Long creatorId,
          Long foodCategoryId,
          String title,
          String description,
          Integer minParticipants,
          Integer maxParticipants,
          LocalDateTime orderExpectedAt,
          PartyStatus status,
          LocalDateTime closedAt,
          SettlementStatus settlementStatus,
          Integer settlementTotalAmount,
          LocalDateTime settlementRequestedAt,
          Long settlementBankAccountId,
          LocalDateTime createdAt,
          LocalDateTime updatedAt
  ) {
    this.creatorId = creatorId;
    this.foodCategoryId = foodCategoryId;
    this.title = title;
    this.description = description;
    this.minParticipants = minParticipants;
    this.maxParticipants = maxParticipants;
    this.orderExpectedAt = orderExpectedAt;
    this.status = status;
    this.closedAt = closedAt;
    this.settlementStatus = settlementStatus;
    this.settlementTotalAmount = settlementTotalAmount;
    this.settlementRequestedAt = settlementRequestedAt;
    this.settlementBankAccountId = settlementBankAccountId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public void requestSettlement(int totalAmount, Long bankAccountId, LocalDateTime requestedAt) {
    if (settlementStatus != SettlementStatus.NONE && settlementStatus != SettlementStatus.CANCELED) {
      throw new IllegalStateException("정산 요청을 시작할 수 없는 상태입니다.");
    }
    this.settlementStatus = SettlementStatus.REQUESTED;
    this.settlementTotalAmount = totalAmount;
    this.settlementBankAccountId = bankAccountId;
    this.settlementRequestedAt = requestedAt;
  }

  public void completeSettlement() {
    if (settlementStatus != SettlementStatus.REQUESTED) {
      throw new IllegalStateException("진행 중인 정산만 완료할 수 있습니다.");
    }
    this.settlementStatus = SettlementStatus.COMPLETED;
  }

  public void cancelSettlement() {
    if (settlementStatus != SettlementStatus.REQUESTED) {
      throw new IllegalStateException("진행 중인 정산만 취소할 수 있습니다.");
    }
    this.settlementStatus = SettlementStatus.CANCELED;
  }

  public void completeDelivery() {
    this.status = PartyStatus.COMPLETED;
    this.updatedAt = LocalDateTime.now();
  }

  public void completeOrder() {
    this.status = PartyStatus.ORDERED;
    this.updatedAt = LocalDateTime.now();
  }

  public void update(
          String title,
          String description,
          Integer maxParticipants,
          LocalDateTime orderExpectedAt
  ) {
    this.title = title;
    this.description = description;
    this.maxParticipants = maxParticipants;
    this.orderExpectedAt = orderExpectedAt;
    this.updatedAt = LocalDateTime.now();
  }
}
