# BlogTree_AI

Repository for AI-driven Technical Blog Summarization and Knowledge Graph Visualization

# Git Convention

### 기본 흐름 (GitHub Flow)

1. `main` 브랜치는 항상 배포 가능한 상태 유지
2. 새로운 작업은 `feature/*` 브랜치에서 진행
3. 작업 완료 후 Pull Request 생성
4. 코드 리뷰 후 `main`으로 merge

# Branch Naming Convention

### 1. 기본구조

```bash
type/short-description
```

- Branch Type
    
    개발 시에는 feature type하면 됨. 기능 하나마다 feature branch 하나
    
    | Type | Description |
    | --- | --- |
    | `feature` | 새로운 기능 개발 |
    | `fix` | 버그 수정 |
    | `refactor` | 리팩토링 (기능 변화 없음) |
    | `docs` | 문서 수정 |
    | `test` | 테스트 코드 추가/수정 |
    | `chore` | 빌드 설정, 패키지 수정 등 |
    | `hotfix` | 긴급 배포 수정 |
- Example

```bash
feature/user-login
feature/payment-api
fix/jwt-token-expiration
refactor/auth-service
docs/readme-update
```

### 2. 규칙

- 소문자 사용
- 공백 대신 `-`사용
- 최대한 간결하고 목적이 드러나도록 작성
- 하나의 브랜치는 하나의 작업만 담당

# Commit Message Convention

### 1. 기본 구조

description은 굳이 안해도 됨.

```bash
type: short summary

(optional) detailed description
```

- Type 종류
    
    개발 시 기능 하나마다 feat type으로 커밋. 추후 fix나 refactor, test 사용
    
    | Type | Description |
    | --- | --- |
    | `feat` | 새로운 기능 추가 |
    | `fix` | 버그 수정 |
    | `refactor` | 코드 구조 개선 |
    | `docs` | 문서 수정 |
    | `test` | 테스트 코드 |
    | `chore` | 설정, 빌드 관련 |
    | `style` | 코드 스타일 변경 (로직 변경 없음) |
    | `perf` | 성능 개선 |

### 2. 규칙

- 한 커밋은 하나의 논리적 변경만 포함
- 커밋 메시지는 영어로 작성
- 요약은 50자 이내
- 명령형으로 작성 (`add`, `fix`, `update`)
- 마침표 사용하지 않음

# Pull Request Convention

### 1. Pull Request란

: 내 브랜치의 변경 내용을 대상 브랜치에 합쳐달라고 요청하는 것

- example
feature/login  →  main

### 2. PR 흐름

1️⃣ 브랜치 생성

2️⃣ 기능 개발

3️⃣ commit & push

4️⃣ GitHub에서 Pull Request 생성

5️⃣ 코드 리뷰

6️⃣ 수정 요청 반영

7️⃣ CI 통과 ← 이건 나중에 Github actions 도입 후

8️⃣ merge

### 3. 기본 구조

```bash
[type] short summary

// example
[feat] implement user registration API
[fix] resolve token validation bug
```
