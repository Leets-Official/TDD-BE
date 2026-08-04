# EC2에서 RDS 직접 접속해 기숙사 인증 승인하기 (환경변수 로딩 실패 포함)

## 환경

- AWS EC2: Amazon Linux, t3.micro
- AWS RDS: MySQL 8
- Docker: 앱 컨테이너 `tdd-api` 구동 중
- 셸: bash
- 작업일: 2026-08-04

---

## 배경

기숙사 인증 승인(PENDING → APPROVED)은 MVP 범위에서 API를 만들지 않고 관리자가 `dormitory` 테이블을 직접 UPDATE하는 방식으로 확정되어 있다. 관리자 인증/권한 체계가 아직 없기 때문이다. 절차와 SQL 템플릿은 [dormitory-verification-admin-guide.md](../../operations/dormitory-verification-admin-guide.md)에 정리돼 있다.

이 문서는 그 작업을 **EC2에 접속해 실제로 수행한 기록**이다. 로컬에서 RDS로 직접 붙을 수 없는 환경(RDS가 EC2 보안그룹만 허용)이라 EC2를 경유해야 했고, 그 과정에서 DB 접속 정보를 셸에 불러오다 막힌 지점이 있어 함께 남긴다.

---

## 증상

앱 컨테이너의 환경변수를 셸로 불러온 뒤 mysql 접속을 시도했는데, 호스트를 지정했는데도 로컬 소켓으로 접속하려다 실패했다.

```
ERROR 2002 (HY000): Can't connect to local MySQL server through socket '/var/lib/mysql/mysql.sock' (2)
```

`-h "$DB_HOST"`를 분명히 넘겼는데도 원격이 아닌 로컬을 보고 있었다. docker로 mysql 클라이언트를 띄워 재시도해도 같은 에러였다.

```
ERROR 2002 (HY000): Can't connect to local MySQL server through socket '/var/run/mysqld/mysqld.sock' (2)
```

---

## 원인

환경변수를 불러올 때 쓴 명령이 문제였다.

```bash
# 문제가 된 명령
eval "$(docker inspect tdd-api --format '{{range .Config.Env}}{{println .}}{{end}}' | grep '^SPRING_DATASOURCE_' | sed 's/^/export /')"
```

`sed 's/^/export /'`는 `KEY=VALUE` 앞에 `export`만 붙인다. 값을 따옴표로 감싸지 않기 때문에, `eval`이 값 안의 셸 메타문자를 그대로 해석한다.

JDBC URL에는 쿼리 파라미터 구분자 `&`가 들어 있다.

```
jdbc:mysql://<rds-endpoint>:3306/ttdd?serverTimezone=Asia/Seoul&...
```

`eval`은 이 `&`를 **백그라운드 실행 연산자**로 읽는다. 실행 결과에 잡 번호가 찍힌 것이 증거다.

```
[1] 1563096
[1]+  Done    export SPRING_DATASOURCE_URL=jdbc:mysql://<rds-endpoint>:3306/ttdd?serverTimezone=Asia/Seoul
```

`export`가 백그라운드 서브셸에서 실행됐으므로 **현재 셸에는 값이 남지 않는다.** 그래서 파싱 결과가 전부 비었다.

```
host= port=3306 db= user=<db-user>
```

`host`와 `db`가 비었다. `SPRING_DATASOURCE_USERNAME`만 제대로 들어온 이유는 그 값에 `&`가 없어서다.

여기서 mysql은 `-h ""`처럼 빈 호스트를 받으면 인자를 무시하고 기본값인 로컬 소켓으로 접속한다. 그래서 원격 주소를 넘겼다고 생각한 상태에서 로컬 접속 에러를 보게 된다.

**에러 메시지가 원인 지점과 멀어서 헷갈리기 쉽다.** 실제 문제는 mysql이 아니라 그 앞의 변수 로딩이었다.

---

## 해결

`eval`을 쓰지 않고 `$( )`로 값을 직접 변수에 담는다. 명령 치환 결과는 셸이 다시 해석하지 않으므로 `&`, `?` 같은 문자가 있어도 안전하다.

```bash
DB_PASS=$(docker inspect tdd-api --format '{{range .Config.Env}}{{println .}}{{end}}' \
  | grep '^SPRING_DATASOURCE_PASSWORD=' | cut -d= -f2-)
DB_HOST=$(docker inspect tdd-api --format '{{range .Config.Env}}{{println .}}{{end}}' \
  | grep '^SPRING_DATASOURCE_URL=' | cut -d= -f2- | sed -E 's|jdbc:mysql://||; s|[:/?].*||')
DB_NAME=$(docker inspect tdd-api --format '{{range .Config.Env}}{{println .}}{{end}}' \
  | grep '^SPRING_DATASOURCE_URL=' | cut -d= -f2- | sed -E 's|jdbc:mysql://[^/]+/||; s|\?.*||')
DB_USER=$(docker inspect tdd-api --format '{{range .Config.Env}}{{println .}}{{end}}' \
  | grep '^SPRING_DATASOURCE_USERNAME=' | cut -d= -f2-)

echo "host=$DB_HOST db=$DB_NAME user=$DB_USER pass=${DB_PASS:+설정됨}"
```

마지막 줄은 값이 비지 않았는지 확인하는 용도다. `${DB_PASS:+설정됨}`은 값이 있을 때만 "설정됨"을 출력하므로 **비밀번호 자체는 화면에 찍히지 않는다.**

`cut -d= -f2-`에서 `-f2-`(2번째 필드부터 끝까지)를 쓰는 이유는 값 안에 `=`가 또 들어갈 수 있기 때문이다. `-f2`만 쓰면 `?serverTimezone=Asia/Seoul`에서 잘린다.

### 전체 작업 절차

EC2에는 `/usr/bin/mysql`이 이미 설치돼 있어 docker로 클라이언트를 띄울 필요가 없다. 먼저 확인한다.

```bash
command -v mysql || echo "없음"
```

**1. 접속 정보 로딩** — 위 해결 코드 블록 실행

**2. 대상 확인** — 엉뚱한 계정을 승인하지 않도록 이메일까지 조인해서 본다.

```bash
MYSQL_PWD="$DB_PASS" mysql -h "$DB_HOST" -u "$DB_USER" "$DB_NAME" -e "
SELECT d.user_id, u.email, d.dormitory, d.dorm_status, d.dorm_verification_image_key
FROM dormitory d JOIN users u ON u.id = d.user_id
WHERE d.user_id = :userId;"
```

`dorm_status`가 `PENDING`인지, 이메일이 요청받은 계정과 일치하는지 확인한다. `dorm_verification_image_key`로 S3에서 인증 사진을 확인하고 승인 여부를 판단한다.

**3. 만료일 계산** — 손으로 계산하지 말고 계산기 API를 쓴다.

```bash
curl -s "http://127.0.0.1:8080/api/v1/internal/dorm-semester-end"
```

3~8월 인증은 그 해 8/31 23:59:59, 9월~다음해 2월 인증은 학기가 끝나는 해의 2월 말일 23:59:59로 계산된다.

**4. 승인 UPDATE**

```bash
MYSQL_PWD="$DB_PASS" mysql -h "$DB_HOST" -u "$DB_USER" "$DB_NAME" -e "
UPDATE dormitory
SET dorm_status = 'APPROVED',
    dorm_verified_at = NOW(),
    dorm_verified_until = ':dormVerifiedUntil',
    reject_reason = NULL
WHERE user_id = :userId AND dorm_status = 'PENDING';
SELECT ROW_COUNT() AS changed;"
```

`changed`가 1이어야 정상이다. 0이면 이미 PENDING이 아니라는 뜻이므로 2번부터 다시 확인한다.

**5. 결과 확인**

```bash
MYSQL_PWD="$DB_PASS" mysql -h "$DB_HOST" -u "$DB_USER" "$DB_NAME" -e "
SELECT user_id, dorm_status, dorm_verified_at, dorm_verified_until, reject_reason
FROM dormitory WHERE user_id = :userId;"
```

**6. 정리**

```bash
unset DB_PASS
```

---

## 주의할 점

### dorm_status만 바꾸면 안 된다

`Dormitory.approve()`가 실제로 세팅하는 필드는 네 개다.

```java
public void approve(LocalDateTime until) {
    this.dormStatus = DormStatus.APPROVED;
    this.dormVerifiedAt = LocalDateTime.now();
    this.dormVerifiedUntil = until;
    this.rejectReason = null;
}
```

`dorm_verified_until`을 비워두면 만료 스케줄러가 그 row를 영영 찾지 못한다.

```java
// DormVerificationExpiryService
findByDormStatusAndDormVerifiedUntilBefore(DormStatus.APPROVED, LocalDateTime.now())
```

`NULL`은 `Before` 조건에 걸리지 않으므로 **영구 승인 상태**가 된다. 수동 UPDATE는 앱 로직이 하는 일을 사람이 대신하는 것이므로, 어떤 필드를 함께 바꿔야 하는지 도메인 코드를 먼저 확인해야 한다.

### WHERE 절의 상태 조건을 빼지 말 것

```sql
WHERE user_id = :userId AND dorm_status = 'PENDING'
```

`dorm_status = 'PENDING'`은 이미 처리된 건을 실수로 다시 덮어쓰는 것을 막는 안전장치다. `ROW_COUNT()`가 0으로 나오면 "안 걸렸다"는 신호를 주므로, 조건 없이 실행해 조용히 덮어쓰는 것보다 낫다.

### 비밀번호를 명령줄에 직접 넣지 말 것

`mysql -p비밀번호` 형태는 같은 서버의 다른 프로세스에서 `ps`로 볼 수 있다. `MYSQL_PWD` 환경변수를 쓰면 프로세스 목록에 노출되지 않는다.

### 저장되는 시각은 UTC다

RDS 서버 타임존과 앱 컨테이너(`eclipse-temurin:21-jre`, 별도 `TZ` 미설정)가 모두 UTC라, `NOW()`와 앱의 `LocalDateTime.now()`가 같은 기준으로 기록된다. 수동 UPDATE와 앱 처리 결과가 일관되므로 문제는 없지만, DB를 눈으로 볼 때 한국 시간과 9시간 차이가 난다는 점은 알고 있어야 한다.

확인 방법:

```bash
MYSQL_PWD="$DB_PASS" mysql -h "$DB_HOST" -u "$DB_USER" -e "SELECT NOW(), @@session.time_zone, @@global.time_zone;"
```

---

## 핵심 요약

값에 `&`나 `?`가 들어 있는 환경변수는 `eval "... | sed 's/^/export /'"`로 불러오면 셸이 메타문자로 해석해 조용히 실패한다. `$( )`로 직접 담아야 하며, 빈 `-h` 인자를 받은 mysql이 로컬 소켓 에러를 내기 때문에 원인이 엉뚱한 곳처럼 보인다.
