package com.leets.tdd.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
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

  public BankAccount(Long userId, String bankName, String accountNumber, String accountHolder) {
    this.userId = userId;
    this.bankName = bankName;
    this.accountNumber = accountNumber;
    this.accountHolder = accountHolder;
    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  private void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  // 마이페이지 > 계좌 수정에서 사용. userId는 등록 시 이미 확정된 값이라 여기서 바꾸지 않는다.
  public void update(String bankName, String accountNumber, String accountHolder) {
    this.bankName = bankName;
    this.accountNumber = accountNumber;
    this.accountHolder = accountHolder;
  }
}
