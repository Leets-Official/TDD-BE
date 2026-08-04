# 기숙사 인증 승인/반려 운영 가이드 (MVP: 관리자 DB 직접 UPDATE)

FR-AUTH-02 확정 사항: 기숙사 인증 승인(PENDING → APPROVED/REJECTED)은 MVP 범위에서
API를 만들지 않고, DB 접근 권한이 있는 관리자가 `dormitory` 테이블을 직접 `UPDATE`해서
처리한다(별도 관리자 인증/권한 체계가 아직 없어서다). 이 문서는 그 수동 작업을 실수 없이
하기 위한 SQL 템플릿과 절차를 정리한다.

> **주의**: 아래 `:userId`, `:dormVerifiedUntil` 등은 MyBatis/JDBC 같은 바인딩 도구의
> named parameter가 아니다 - MySQL Workbench/DBeaver 같은 일반 SQL 클라이언트에 사람이
> 직접 값을 채워 넣는 자리표시자다. 숫자(`:userId`)는 따옴표 없이, 문자열/날짜(`:dormVerifiedUntil`)는
> 값 자체를 작은따옴표로 감싸서 치환한다(플레이스홀더를 감싸고 있는 따옴표는 그대로 두고
> 안의 텍스트만 바꾸는 것과 동일). 예시는 각 섹션 하단 참고.

## 사전 확인

1. 승인/반려 대상 유저의 `dormitory` row가 `dorm_status='PENDING'`인지 먼저 확인한다.
   ```sql
   SELECT id, user_id, dormitory, dorm_status, dorm_verification_image_key
   FROM dormitory
   WHERE user_id = :userId;
   ```
2. `dorm_verification_image_key` 값으로 S3(비공개 버킷)에서 실제 인증 사진을 확인하고 승인/반려를 판단한다.

## 승인 처리

1. `dorm_verified_until`에 넣을 값을 아래 API로 구한다(인증 불필요, 데이터 조회/변경 없는 순수 계산기).
   ```
   GET /api/v1/internal/dorm-semester-end?date=2026-07-25
   → { "success": true, "data": { "dorm_verified_until": "2026-08-31T23:59:59" } }
   ```
   `date`를 생략하면 오늘 날짜 기준으로 계산한다. 승인 처리 "오늘" 날짜 기준으로 계산하면 된다
   (인증 사진이 언제 제출됐는지가 아니라 관리자가 승인하는 시점 기준).
2. 아래 SQL의 `:userId`, `:dormVerifiedUntil`을 채워 실행한다.
   ```sql
   UPDATE dormitory
   SET dorm_status = 'APPROVED',
       dorm_verified_at = NOW(),
       dorm_verified_until = ':dormVerifiedUntil',  -- 1번에서 받은 값 그대로
       reject_reason = NULL
   WHERE user_id = :userId
     AND dorm_status = 'PENDING';
   ```
   `WHERE`에 `dorm_status = 'PENDING'`을 같이 걸어서, 이미 처리된 건을 실수로 다시 승인하는 걸 막는다.
   영향받은 row 수가 0이면 이미 PENDING이 아니라는 뜻이니 1번부터 다시 확인한다.

   실제 값을 채운 예시(userId=42, 1번 응답의 dorm_verified_until="2026-08-31T23:59:59"):
   ```sql
   UPDATE dormitory
   SET dorm_status = 'APPROVED',
       dorm_verified_at = NOW(),
       dorm_verified_until = '2026-08-31 23:59:59',
       reject_reason = NULL
   WHERE user_id = 42
     AND dorm_status = 'PENDING';
   ```

## 반려 처리

```sql
UPDATE dormitory
SET dorm_status = 'REJECTED',
    reject_reason = ':rejectReason'  -- 실제 사유 문자열로 교체(따옴표는 유지)
WHERE user_id = :userId
  AND dorm_status = 'PENDING';
```

실제 값을 채운 예시(userId=42):
```sql
UPDATE dormitory
SET dorm_status = 'REJECTED',
    reject_reason = '사진이 흐릿해서 동/호수를 확인할 수 없습니다'
WHERE user_id = 42
  AND dorm_status = 'PENDING';
```

반려 건은 재제출을 유도하기 위해 인증 사진을 즉시 삭제하지 않는다(별도 배치나 수동 삭제 불필요).

## 참고

- 로컬에서 RDS로 직접 붙을 수 없는 환경이면 EC2를 경유해야 한다. 접속 정보를 셸로 불러오는
  방법과 실제 실행 명령은 [2026-08-04-dorm-approval-on-ec2.md](../troubleshooting/md/2026-08-04-dorm-approval-on-ec2.md)에
  정리해두었다(JDBC URL의 `&` 때문에 `eval`로 환경변수를 로딩하면 실패하는 함정 포함).
- `dorm_verified_at`은 반려 시 건드리지 않는다(승인된 적 있으면 그 기록을 유지, 없으면 계속 NULL).
- 학기 만료(`dorm_verified_until` 경과) 처리는 수동 작업이 아니라 매일 00:00 배치
  (`DormVerificationExpiryScheduler`)가 자동으로 `EXPIRED` 전환 + S3 이미지 삭제까지 처리한다.
- 이 수동 UPDATE 방식은 승인/반려 물량이 늘어나거나 관리자 권한 체계(role)가 생기면
  정식 관리자 승인 API로 교체를 검토한다.
