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
}
