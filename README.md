# MindMap Study

PDF 교재를 업로드하면 AI가 핵심 개념과 연결 관계를 분석해서
인터랙티브 마인드맵으로 자동 생성해주는 학습 도우미입니다.

```
운영체제.pdf 업로드
        ↓
    AI 분석 (OpenAI)
        ↓
 [디스크 스케줄링]
   /    |    \
[FCFS][SSTF][SCAN]  ← 클릭하면 개념 설명 표시
```

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 백엔드 | Spring Boot 4.0.3, Java 17 |
| DB | PostgreSQL (포트 5433) |
| 캐시 | Redis |
| 메시지 큐 | Apache Kafka |
| PDF 파싱 | Apache PDFBox 3.0 |
| AI | OpenAI API |
| 프론트엔드 | React 18 + TypeScript + Vite |
| 그래프 시각화 | vis-network |

---

## 사전 준비

### 1. Java 17

```bash
java -version  # 17.x.x 이상이면 OK
```

없으면 https://adoptium.net 에서 **Temurin 17** 설치

### 2. Docker Desktop

https://www.docker.com/products/docker-desktop 에서 설치 후 실행

```bash
docker -v  # 설치 확인
```

### 3. Node.js 18 이상

https://nodejs.org → LTS 버전 설치

```bash
node -v  # 설치 확인
```

### 4. OpenAI API Key

https://platform.openai.com 에서 API 키 발급 후 아래 설정 파일에 입력

---

## 최초 설정 (처음 한 번만)

### Step 1 — 환경 변수 파일 생성

`src/main/resources/` 아래에 `application-local.yml` 파일을 생성합니다.
(`application-local.yml.example` 참고)

```yaml
spring:
  datasource:
    username: myuser
    password: secret

openai:
  api-key: sk-xxxxxxxxxxxxxxxx   # 발급받은 OpenAI API 키
  model: gpt-4o-mini             # 사용할 모델명
```

### Step 2 — 프론트엔드 패키지 설치

```bash
cd frontend
npm install
```

---

## 실행 방법

터미널 3개를 열어서 각각 실행합니다.

### 터미널 1 — Docker (PostgreSQL + Redis + Kafka)

```bash
docker compose up
```

> 처음 실행 시 이미지 다운로드로 시간이 걸립니다.

아래 로그가 보이면 준비 완료:
```
✔ Container demo-postgres-1  Started
✔ Container demo-redis-1     Started
✔ Container demo-kafka-1     Started
```

### 터미널 2 — 백엔드 (Spring Boot)

```bash
./gradlew bootRun
```

Windows:
```bash
gradlew.bat bootRun
```

아래 로그가 보이면 준비 완료:
```
Started DemoApplication in X.XXX seconds
```

### 터미널 3 — 프론트엔드 (React)

```bash
cd frontend
npm run dev
```

아래 로그가 보이면 준비 완료:
```
➜  Local:   http://localhost:3000/
```

### 브라우저 접속

```
http://localhost:3000
```

---

## 사용 방법

1. **홈 화면**에서 공부할 PDF 파일을 드래그하거나 클릭해서 선택
2. 과목명 입력 (예: `운영체제`, `자료구조`)
3. 상세도 레벨 선택

| 레벨 | 주요 개념 | 세부 개념 | 특징 |
|------|---------|---------|------|
| SIMPLE | 3개 | 2개씩 | 빠른 분석 |
| NORMAL | 5개 | 4개씩 | 기본값 |
| DETAILED | 7개 | 5개씩 | 상세 분석 |

4. **마인드맵 생성** 버튼 클릭
5. AI 분석 완료 후 그래프 자동 표시
6. **노드 클릭** → 우측 패널에 개념 설명 + 연결 관계 표시
7. 마우스 스크롤로 확대/축소, 드래그로 이동 가능

> AI 분석 시간: PDF 크기 및 레벨에 따라 **10초 ~ 1분** 소요

---

## 폴더 구조

```
demo/
├── src/main/java/com/example/demo/
│   ├── ai/                   ← OpenAI API 연동 (개념 추출)
│   ├── config/               ← Security, Kafka, Redis 설정
│   ├── domain/
│   │   ├── mindmap/          ← 마인드맵, 노드, 엣지 (Entity/Service/Controller)
│   │   └── pdf/              ← PDF 업로드 및 파싱
│   └── kafka/                ← 비동기 AI 처리 (Producer/Consumer)
├── src/main/resources/
│   ├── application.yml           ← 공통 설정
│   └── application-local.yml     ← 민감 정보 (git 제외)
├── frontend/                 ← React 프론트엔드
│   └── src/
│       ├── api/              ← 백엔드 API 호출
│       ├── components/       ← PdfUpload, MindMapGraph, NodeDetail
│       ├── pages/            ← HomePage, MindMapPage
│       └── types/            ← TypeScript 타입 정의
└── compose.yaml              ← Docker 컨테이너 설정
```

---

## API 엔드포인트

| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/pdf/upload` | PDF 업로드 (form-data: file, subject, detailLevel) |
| GET | `/api/mindmap/{id}` | 마인드맵 단건 조회 (노드+엣지 포함) |
| GET | `/api/mindmap/list` | 전체 마인드맵 목록 |
| DELETE | `/api/mindmap/{id}` | 마인드맵 삭제 |

---

## 자주 발생하는 문제

### DB 연결 오류 (`bootRun` 실행 시)

Docker가 완전히 뜨기 전에 Spring Boot를 실행하면 발생합니다.
터미널 1에서 `Started` 로그 확인 후 Spring Boot를 실행하세요.

### `npm install` 이후 `vis-network` 관련 오류

```bash
cd frontend
npm install --legacy-peer-deps
```

### 포트 충돌 (EADDRINUSE)

| 포트 | 서비스 |
|------|--------|
| 3000 | React 프론트엔드 |
| 8080 | Spring Boot 백엔드 |
| 5433 | PostgreSQL |
| 6379 | Redis |
| 9092 | Kafka |

---

## 종료 방법

- 터미널 1, 2, 3에서 각각 `Ctrl + C`
- Docker 컨테이너 중지:

```bash
docker compose down
```
