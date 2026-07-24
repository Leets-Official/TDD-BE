# 정산 API 운영 기준

## 동시성

- 정산 생성, 송금 완료·되돌리기, 정산 완료·취소는 `delivery_parties` 행에 비관적 쓰기 잠금을 획득한 뒤 처리한다.
- `delivery_parties.version`과 `party_participants.version`은 낙관적 잠금으로 예상하지 못한 동시 수정도 감지한다.
- 잠금을 획득하지 못하면 `409 Conflict`와 재시도 안내를 반환한다.

## 관측과 감사

- 정산 상태 변경은 `partyId`, 요청 사용자 ID, 처리 결과 수치를 애플리케이션 로그에 남긴다.
- Actuator의 `health`, `info`, `metrics` 엔드포인트를 노출한다. 외부 접근은 인프라 보안 그룹 또는 프록시에서 제한한다.
- `/actuator/metrics`는 운영자 전용이다. 일반 클라이언트에 공개하지 않는다.

## API 문서

- Swagger UI에서 `Settlement` 태그로 정산 API를 확인한다.
- 모든 정산 API는 `bearerAuth` 인증이 필요하다.

## 후속 협업 결정

- 모바일·웹 재시도까지 같은 성공 응답을 보장하려면 요청별 idempotency key의 전달 형식과 보관 기간을 먼저 합의한다.
- 정산 생성·완료 이벤트를 채팅 카드나 알림으로 전달하려면 공용 outbox 테이블, 발행 재시도 정책, 소비자 소유 도메인을 정한 뒤 구현한다.
