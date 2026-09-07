# 보안입찰 RFP-스펙 매칭 시스템

공공기관 입찰공고를 Open API로 자동 수집하고, RFP 요구사항을 자사 보안장비 스펙과 대조해
**충족 / 부분충족 / 미충족 / 확인불가** 갭분석표를 자동으로 만들어 주는 시스템입니다.

네트워크 보안장비(IPS/IDS, DDoS 대응) 기술영업의 제안서 대응 업무에서, 공고 하나마다
"처리성능 10Gbps 이상", "CC인증 EAL2 이상", "보안적합성 검증필" 같은 요구사항을
제품 카탈로그와 한 줄씩 대조하던 작업을 코드로 옮긴 것입니다.

> 개인 학습용 포트폴리오 프로젝트입니다. 코드에 등장하는 기관명·제품명은 모두 가상이며,
> 연동 대상 Open API의 엔드포인트와 인증키는 저장소에 두지 않고 환경변수로만 주입합니다.

---

## 1. 핵심 기능

| 기능 | 설명 |
|---|---|
| 입찰공고 자동 수집 | 공공 입찰공고 Open API를 키워드(침입방지·방화벽·DDoS 등)로 주기 폴링해 DB에 캐싱 |
| 요구사항 등록 | RFP 첨부파일은 API로 구조화되어 오지 않으므로, 담당자가 원문을 읽고 항목을 등록하는 반자동 방식 |
| 제품 스펙 관리 | 자사 제품과 스펙 항목 CRUD |
| 매칭 / 갭분석 | 요구사항 ↔ 제품 스펙을 유형별 규칙으로 판정하고 충족률·필수 미충족 건수까지 요약 |

## 2. 기술 스택

- **Backend** — Java 17, Spring Boot 3.4, Spring Data JPA, Spring Scheduler, Bean Validation, springdoc-openapi
- **DB** — H2(로컬 기본) / MySQL 8 (Docker·RDS)
- **Frontend** — React 18, TypeScript, Vite, Axios, Recharts, React Router
- **외부 연동** — 공공 입찰공고 Open API (엔드포인트는 설정 주입)

## 3. 아키텍처

```
[React + TypeScript]
   │ REST/JSON (/api/v1)
   ▼
[Spring Boot API]
   ├─ BidController          공고 목록/상세, 수동 동기화
   ├─ RequirementController  공고별 요구사항 등록/조회/삭제
   ├─ ProductController      자사 제품 스펙 CRUD
   ├─ MatchController        매칭 실행 + 갭분석 결과 조회
   ├─ MatchEngine            ★ 유형별 판정 규칙 (이 프로젝트의 핵심 로직)
   └─ BidSyncScheduler       1일 2회 폴링 → BidAnnouncement upsert
   ▼
[MySQL] BidAnnouncement / RequirementItem / ProductSpec(+Detail) / MatchResult
```

**설계 포인트** — 공고는 실시간 조회 대신 스케줄러로 수집해 DB에 캐싱합니다.
외부 API가 죽어도 화면은 마지막 수집분으로 계속 동작하고, 동기화 실패는 502와 로그로만 드러납니다.

## 4. 실행 방법

### 4.1 로컬 (H2, 인증키 불필요)

```bash
# 백엔드 — http://localhost:8080
cd backend && mvn spring-boot:run

# 프론트엔드 — http://localhost:5173 (/api 요청은 8080으로 프록시)
cd frontend && npm install && npm run dev
```

기본 프로필은 H2 인메모리이며, 기동 시 예시 공고 2건·요구사항 8건·제품 2건이 들어갑니다
(`app.sample-data.enabled=false` 로 끌 수 있음).

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:rfpmatch`)

### 4.2 MySQL / Docker

```bash
# 연동 정보는 저장소에 커밋하지 말고 환경변수(.env)로 주입한다.
export BID_API_BASE_URL="https://<open-api-host>/<service-path>"
export BID_API_OPERATION="<오퍼레이션 경로>"
export BID_API_SERVICE_KEY="<발급받은 인증키>"

docker compose up -d          # MySQL + API
```

`mysql` 프로필에서는 예시 데이터가 들어가지 않고, 공고 수집 스케줄러가 켜집니다.

### 4.3 외부 Open API 설정

| 환경변수 | 설명 |
|---|---|
| `BID_API_BASE_URL` | 서비스 base URL |
| `BID_API_OPERATION` | 오퍼레이션 경로. 업무구분(물품/용역/공사)별로 다름 |
| `BID_API_SERVICE_KEY` | 발급받은 인증키(**URL 인코딩 전 원본**) |

- 인증키의 URL 인코딩은 애플리케이션이 한 번만 수행합니다. 이미 인코딩된 키를 넣으면
  이중 인코딩으로 호출이 실패합니다.
- `bid-api.enabled=true` 이면 스케줄러가 매일 07시·19시(KST)에 수집합니다.
  `POST /api/v1/bids/sync` 로 즉시 수집할 수도 있습니다.
- 오퍼레이션별 파라미터명·응답 필드는 연동 대상 명세에 따라 다릅니다. 필드가 다르면
  `BidNoticeItem` 레코드만 맞춰 주면 됩니다.

## 5. API 요약

Base URL `/api/v1`

| Method | Path | 설명 |
|---|---|---|
| GET | `/bids?keyword=&status=OPEN&page=0&size=20` | 공고 목록 (요구사항 건수·매칭 여부 포함) |
| GET | `/bids/{bidId}` | 공고 상세 + 요구사항 목록 |
| POST | `/bids/sync` | 공고 수동 동기화 |
| POST | `/bids/{bidId}/requirements` | 요구사항 항목 일괄 등록 (201) |
| GET | `/bids/{bidId}/requirements` | 요구사항 목록 |
| DELETE | `/bids/{bidId}/requirements/{id}` | 요구사항 삭제 (204) |
| POST | `/products` · GET `/products` · GET/PUT/DELETE `/products/{id}` | 제품 스펙 CRUD |
| POST | `/bids/{bidId}/match?productId=` | 매칭 실행 → 갭분석표 |
| GET | `/bids/{bidId}/match-result?productId=` | 저장된 갭분석 결과 재조회 |

### 갭분석 응답 예시

```json
{
  "bidId": 1, "productId": 1, "productName": "NGIPS-2000",
  "results": [
    { "requirementId": 1, "category": "처리성능", "status": "충족",
      "requiredValue": "10Gbps", "productValue": "20Gbps",
      "note": "20Gbps / 요구 10Gbps 이상 충족" },
    { "requirementId": 6, "category": "유지보수", "status": "확인불가",
      "requiredValue": "3년", "productValue": null,
      "note": "제품 스펙에 대응 항목이 없습니다 — 스펙 등록 또는 담당자 확인 필요" }
  ],
  "summary": { "totalCount": 6, "satisfied": 5, "partial": 0, "unsatisfied": 0,
               "unknown": 1, "satisfactionRate": 83.3,
               "mandatoryUnsatisfied": 0, "biddable": true }
}
```

### 에러 응답 (공통)

```json
{ "timestamp": "2026-09-07T14:00:00+09:00", "status": 400, "error": "BAD_REQUEST",
  "message": "requiredValue는 비어있을 수 없습니다.", "path": "/api/v1/bids/1/requirements" }
```

| 코드 | 상황 |
|---|---|
| 400 | 입력값 검증 실패, 요구사항 없이 매칭 실행 |
| 404 | 공고·제품·요구사항 ID 없음, 매칭 결과 없음 |
| 502 | 외부 Open API 응답 실패 (마지막 캐시 유지) |

## 6. 매칭 로직

외부 API 연동이 아니라 **직접 설계한 도메인 규칙**이 이 프로젝트의 핵심입니다.

### 6.1 비교 대상 찾기 — `SpecKeyResolver`

RFP 문구와 제품 카탈로그는 항목명이 다릅니다("네트워크 처리량" ↔ "처리성능").
동의어 사전으로 양쪽을 표준 키로 환산해 비교할 스펙을 찾고, 사전에 없으면 항목명이
요구사항 문구에 등장하는지로 판단합니다. 분류("인증")보다 원문 문구가 구체적이므로
**문구를 먼저** 보고, 같은 분류에 성격이 다른 항목이 섞여 있어도 구분합니다.

### 6.2 유형별 판정 규칙 — `MatchRule` 구현체

| 유형 | 규칙 | 예 |
|---|---|---|
| 수치형 | 단위를 기준 단위로 환산 후 부등호 비교. 문구의 "이상/이하"로 비교 방향 결정 | `10Gbps` vs `8000Mbps` → 미충족 |
| 등급형 | EAL1 < … < EAL7 (증강 `EAL4+`는 EAL4와 EAL5 사이) | `EAL2` 요구 / `EAL4` 보유 → 충족 |
| Boolean형 | YES/NO, 유/무, 지원/미지원 등 표기 흔들림 흡수 | 보안적합성 검증필 미보유 → 미충족 |
| 텍스트형 | 값이 같을 때만 충족, 키워드만 겹치면 부분충족, 그 외 확인불가 | 담당자 최종 판단 |

단위 정규화는 전송속도(bps)·패킷처리율(pps)·용량(byte)·초당건수·시간·개수 계열을 지원하고,
`1,000만`, `20Gbps`, `10` + 단위 `Gbps` 같은 표기를 모두 받습니다.
**계열이 다르면 비교하지 않고 "확인불가"로 둡니다.**

### 6.3 보수적으로 판정하는 이유

- 대응 스펙이 아예 없으면 → **미충족이 아니라 확인불가**. 카탈로그에 안 적혀 있을 뿐 실제로는
  지원하는 기능일 수 있고, 이 판단은 담당자 몫입니다.
- 요구치의 10% 이내로 모자란 수치, 한 등급 낮은 CC인증 → **부분충족**. 상위 모델 대체나
  스펙 협의 여지를 남깁니다.
- 잘못된 자동 "충족" 판정은 입찰 실격으로 이어지므로, 애매하면 사람에게 넘깁니다.
- 필수(mandatory) 요구사항에 미충족이 하나라도 있으면 요약의 `biddable`이 false가 됩니다.

## 7. 프로젝트 구조

```
backend/src/main/java/com/rfpmatch/
├── bid/            공고 도메인·조회
├── requirement/    요구사항 등록
├── product/        제품 스펙 CRUD
├── match/
│   ├── engine/     ★ MatchEngine, SpecKeyResolver, UnitNormalizer, 규칙 4종
│   ├── service/    매칭 실행·결과 조회
│   └── domain/     MatchResult, MatchStatus
├── sync/           Open API 클라이언트·동기화 서비스·스케줄러
└── common/         공통 에러 포맷, CORS·OpenAPI·샘플데이터 설정

frontend/src/
├── api/            API 클라이언트 & 응답 타입
├── components/     판정 배지, 갭분석 차트
└── pages/          공고 목록 / 공고 상세(요구사항+갭분석) / 제품 스펙
```

## 8. 테스트

```bash
cd backend && mvn test     # 47건
```

- 단위 정규화·CC등급 파싱·항목 매핑·판정 규칙 단위 테스트
- 공고 등록 → 요구사항 입력 → 제품 등록 → 매칭 → 갭분석 조회까지 API 통합 테스트
- Open API 응답 파싱(배열/`items.item` 래핑/XML 에러/HTTP 오류/미설정)과 동기화 upsert·부분 실패 테스트

## 9. 로드맵

- [x] **Phase 1 (MVP)** — Open API 연동·공고 수집, 제품 스펙 CRUD, 요구사항 등록, 수치/등급/Boolean 매칭
- [x] **Phase 2** — 갭분석 화면(충족률 시각화), 매칭 결과 저장·재조회
- [ ] **Phase 3** — 텍스트형 규칙 확장, 매칭 결과 기반 제안서 초안 내보내기
- [ ] **Phase 4** — AWS EC2 배포, Swagger 문서 공개

## 10. 참고

- CC인증(공통평가기준) 등급과 보안적합성 검증 여부는 자동 수집하지 않습니다.
  공개된 인증 목록을 담당자가 확인해 제품 스펙에 직접 입력하는 방식입니다.
- 연동 대상 Open API의 명세(오퍼레이션·파라미터·응답 필드)는 서비스마다 다르므로,
  실제 운영 전에 발급기관이 제공하는 문서와 대조해야 합니다.
