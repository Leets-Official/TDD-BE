package com.leets.tdd.party.domain;

import com.leets.tdd.settlement.domain.PaymentStatus;
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
@Table(name = "party_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** 한 사용자의 배달팟 참여 정보와 개인별 송금 상태를 저장합니다. */
public class PartyParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  @Column(nullable = false)
  private Long version;

  @Column(name = "party_id", nullable = false)
  private Long partyId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private PartyParticipantRole role;

  @Column(name = "joined_at", nullable = false)
  private LocalDateTime joinedAt;

  @Column(name = "canceled_at")
  private LocalDateTime canceledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PartyParticipantStatus status;

  @Column(name = "settlement_amount")
  private Integer settlementAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", length = 20)
  private PaymentStatus paymentStatus;

  @Column(name = "paid_at")
  private LocalDateTime paidAt;

  public PartyParticipant(
      Long partyId,
      Long userId,
      PartyParticipantRole role,
      PartyParticipantStatus status,
      LocalDateTime joinedAt
  ) {
    this.partyId = partyId;
    this.userId = userId;
    this.role = role;
    this.status = status;
    this.joinedAt = joinedAt;
  }

  public void assignSettlementAmount(int amount) {
    this.settlementAmount = amount;
    this.paymentStatus = PaymentStatus.PENDING;
    this.paidAt = null;
  }

  public void clearSettlement() {
    this.settlementAmount = null;
    this.paymentStatus = null;
    this.paidAt = null;
  }

  public void markPaid(LocalDateTime paidAt) {
    if (paymentStatus != PaymentStatus.PENDING) {
      throw new IllegalStateException("송금 완료 처리할 수 없는 상태입니다.");
    }
    this.paymentStatus = PaymentStatus.PAID;
    this.paidAt = paidAt;
  }

  public void undoPaid() {
    if (paymentStatus != PaymentStatus.PAID) {
      throw new IllegalStateException("되돌릴 송금 완료 내역이 없습니다.");
    }
    this.paymentStatus = PaymentStatus.PENDING;
    this.paidAt = null;
  }

  public boolean isJoined() {
    return status == PartyParticipantStatus.JOINED;
  }

  public void cancel(LocalDateTime canceledAt) {
    if (!isJoined()) {
      throw new IllegalStateException("참여 중인 상태가 아닙니다.");
    }
    this.status = PartyParticipantStatus.CANCELED;
    this.canceledAt = canceledAt;
  }
}
