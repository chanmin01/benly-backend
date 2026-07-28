# Backend 협업 컨벤션
 
팀 개발 시작 전에 맞춰두는 브랜치 · 커밋 · PR 규칙.
 
---
 
## 브랜치 전략
 
### 기본 구조
 
```
타입/이슈번호-간단설명
```
 
예시:
 
```
feat/12-kakao-login
fix/23-refresh-token-null
refactor/30-session-service
```
 
### 규칙
 
- 소문자, 단어 구분은 하이픈(`-`)
- 이슈 번호를 붙여 어떤 이슈 작업인지 연결
- 설명은 **영어**로 짧게 (터미널/GitHub에서 한글 깨짐 방지)
- 항상 `develop`에서 따고, `develop`으로 PR
### 브랜치 역할
 
| 브랜치 | 역할 |
|---|---|
| `main` | 배포 전용. 배포할 때만 전진 (develop → main PR) |
| `develop` | 통합 브랜치. 최신 작업이 모임. 기능 브랜치는 여기서 분기 |
| `feat/*`, `fix/*` 등 | 개인 작업 브랜치. develop에서 따서 develop으로 PR |
 
### 작업 흐름
 
```bash
git checkout develop
git pull origin develop
git checkout -b feat/12-kakao-login   # 새 브랜치 생성 + 이동
# ...작업...
git push origin feat/12-kakao-login   # 원격에 올리고 PR 생성
```
 
---
 
## 커밋 메시지 컨벤션
 
**타입은 영어, 설명은 한글.**
 
```
타입: 설명
```
 
예시:
 
```
feat: 카카오 로그인 API 구현
fix: RefreshToken null 반환 문제 수정
refactor: AuthService 토큰 발급 로직 분리
chore: H2 테스트 의존성 추가
docs: README에 실행 방법 추가
test: AuthService 단위 테스트 추가
```
 
### 규칙
 
- `타입: 설명` — 콜론 뒤 한 칸 띄우기
- 제목은 50자 이내, 마침표 없음
- 내용이 길면 본문 추가 (제목 → 빈 줄 → 본문):
```
feat: 카카오 로그인 API 구현
 
- 인가코드로 kakao_id 조회
- 신규 유저면 자동 회원가입
- JWT access/refresh 발급
```
 
- `Closes #12` — 머지되면 해당 이슈 자동 종료
### 커밋 타입
 
| 타입 | 언제 |
|---|---|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변화 없이 코드 개선 |
| `chore` | 빌드 / 설정 / 의존성 등 잡일 |
| `docs` | 문서 |
| `test` | 테스트 코드 |
| `style` | 포맷팅, 세미콜론 등 (로직 변화 없음) |
 
---
 
## PR 규칙
 
- **`develop`으로** PR (main 아님)
- 제목은 커밋과 동일한 형식: `feat: 카카오 로그인 API`
- PR 템플릿(`.github/PULL_REQUEST_TEMPLATE.md`)을 채운다
- **셀프 머지 금지** — 상대방 리뷰 후 머지 (서로 봐주기)
- 관련 이슈를 `Closes #번호`로 연결
---
 
## 이슈 규칙
 
- 작업 시작 전 이슈 생성 (이슈 템플릿 사용)
- 이슈 번호를 브랜치명 / 커밋 / PR에 연결
