package com.leets.tdd.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "bank_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** 정산 요청 시 방장의 계좌 정보를 참조하기 위한 엔티티입니다. */
public class BankAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Column(name = "bank_name", nullable = false, length = 30)
  private String bankName;

  @Column(name = "account_number", nullable = false, length = 30)
  private String accountNumber;

  @Column(name = "account_holder", nullable = false, length = 30)
  private String accountHolder;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
