# Dashboard UI/UX Redesign

## Goal

- 메인 화면을 "지식 트리 탐색 + 비동기 작업 추적 + 추천 탐색"의 단일 워크스페이스로 재구성한다.
- 사용자는 분석 요청 후 화면이 멈춘 것으로 오해하지 않아야 한다.
- SSE 기반 완료 이벤트를 기다리는 동안에도 현재 시스템이 살아 있고 작업이 진행 중이라는 확신을 받아야 한다.

## Screen Architecture

- 기존 글로벌 `AppLayout`은 유지한다.
- 대시보드 페이지 내부를 전용 2열 워크스페이스로 재구성한다.
- 좌측은 `Control Center`, 우측은 `Main Canvas`로 고정한다.

### Desktop Layout

```text
+----------------------------------------------------------------------------------+
| Topbar                                                                           |
+--------------------------------------+-------------------------------------------+
| Control Center                       | Main Canvas                               |
|                                      |                                           |
| 1. URL Input                         | Tree Explorer Header                      |
|    - URL field                       | - active node breadcrumb                  |
|    - Analyze button                  | - zoom controls                           |
|    - live SSE status                 | - fit/reset view                          |
|                                      |                                           |
| 2. Activity List                     | Interactive Knowledge Tree                |
|    - processing items                | - pan                                     |
|    - failed items                    | - zoom                                    |
|    - retry action                    | - node hover                              |
|                                      | - node select                             |
| 3. Recommendation Cards              | - focus transition on newly added node    |
|    - keyword                         |                                           |
|    - topic/category                  | Node Detail Dock                          |
|    - reason preview                  | - selected node summary                   |
|                                      | - related keywords                        |
+--------------------------------------+-------------------------------------------+
```

### Tablet / Mobile Layout

- `Main Canvas`를 최상단으로 올린다.
- `Control Center`는 캔버스 아래로 내려와 세로 스택으로 배치한다.
- 모바일에서는 `Activity List`를 접을 수 있는 아코디언으로 처리한다.
- 추천 카드 영역은 2열 그리드에서 1열 카드 리스트로 축소한다.

## Information Hierarchy

### 1. Main Canvas

- 화면의 주인공은 트리다. 대시보드 진입 시 가장 먼저 보이는 영역이어야 한다.
- 기본 상태에서는 전체 트리 overview를 보여준다.
- 노드 선택 시 우측 하단 또는 캔버스 내부 오버레이에 선택 노드 상세 정보를 노출한다.

#### Canvas Header

- `현재 포커스 경로`: `Category / Topic / Keyword`
- `전체 노드 수`
- `마지막 업데이트 시각`
- 컨트롤:
  - `Zoom In`
  - `Zoom Out`
  - `Fit to Screen`
  - `Reset Focus`

#### Node Interaction

- Hover: 노드 강조, 연결선 밝기 증가, 툴팁 노출
- Click: 노드 선택, 상세 패널 업데이트
- Double Click 또는 CTA: 해당 노드로 포커스 줌
- 새 결과 반영 시: 새 노드가 포함된 영역으로 자동 이동 + 500~800ms 정도의 부드러운 focus transition

### 2. Control Center

사이드바는 단순 입력폼이 아니라 "작업을 시작하고, 기다리고, 결과를 받아들이는 운영 패널" 역할을 맡는다.

#### A. URL Input Section

- 제목: `새 지식 분석`
- 설명: URL을 넣으면 블로그 내용을 분석해 트리에 반영한다.
- 구성:
  - URL 입력 필드
  - `분석 시작` 버튼
  - 입력 유효성 메시지
  - SSE 연결 상태 배지

#### B. Activity List

- 제목: `분석 대기 / 진행 작업`
- 설명: 현재 사용자가 기다리고 있는 비동기 분석 작업을 시간 역순으로 보여준다.
- 작업 카드 필드:
  - URL 또는 도메인
  - 생성 시각
  - 상태 텍스트
  - 스피너 또는 에러 아이콘
  - 실패 시 `재시도` 버튼

#### C. Recommendation Section

- 제목: `추천 키워드`
- 카드형 레이아웃으로 노출한다.
- 카드 정보:
  - keyword
  - category / topic
  - 추천 이유 2줄 미리보기
  - `트리에서 보기` 액션

## UX State Design

### Idle

- 빈 캔버스가 아니라 기존 트리 overview를 보여준다.
- 사이드바 URL 섹션에는 짧은 힌트 문구를 둔다.
  - 예: `URL을 분석하면 새 키워드가 트리에 연결됩니다.`

### Processing

중간 상태 정보가 부족하므로, 진행률 퍼센트 대신 "시스템이 살아 있다"는 신뢰 신호를 설계한다.

- 분석 요청 직후 `Activity List` 최상단에 새 항목을 즉시 추가한다.
- 상태 문구는 고정:
  - `지식을 분석하고 트리를 구성 중입니다...`
- 해당 항목에는 애니메이션 스피너를 표시한다.
- 전체 화면 레벨에서도 SSE 활성 상태를 보인다.
  - URL 입력 섹션 또는 Activity 헤더에 `Live` 배지
  - 배지는 pulse 효과 사용
- 캔버스는 계속 조작 가능해야 한다.
- 단, 최근 추가 예정 영역이 있으면 캔버스 상단에 작은 안내를 둔다.
  - 예: `새 분석 결과를 기다리는 중`

### On Result Event: Success

- SSE 성공 이벤트 수신 즉시 트리 데이터를 refetch 한다.
- refetch 완료 후:
  - 새로 추가된 카테고리/토픽/키워드 노드 찾기
  - 해당 노드로 카메라 이동
  - 노드 pulse highlight 1회 실행
  - Activity item 상태를 `반영 완료`로 교체
- 동시에 추천 카드가 관련 이벤트라면 Recommendation 영역도 즉시 갱신한다.
- 성공 피드백은 토스트보다 "캔버스 이동 + 노드 강조"가 우선이다.

### On Result Event: Failure

- 실패 항목 카드는 붉은 톤 배경과 보더로 강조한다.
- 상태 문구:
  - `분석에 실패했습니다. 다시 시도해 주세요.`
- 카드 하단에 `재시도` 버튼 노출
- 실패한 항목은 즉시 사라지지 않고 사용자가 액션할 때까지 리스트에 남긴다.

### SSE Disconnected / Reconnecting

- 연결 끊김은 task 실패와 별도로 보여줘야 한다.
- 상태 배지 문구:
  - 연결 정상: `Live`
  - 재연결 시도: `Reconnecting`
  - 연결 끊김: `Offline`
- `Offline` 상태에서도 기존 processing item은 유지하되, 상단 안내 문구 추가:
  - `실시간 연결이 불안정합니다. 결과 반영이 지연될 수 있습니다.`

## Visual Direction

현재 다크 톤 기반 방향은 유지하되, 대시보드만 더 구조적으로 보이게 만든다.

- 캔버스는 가장 넓고 가장 어두운 판넬로 만든다.
- 사이드바는 캔버스보다 한 톤 밝은 레이어로 구분한다.
- 상태 색상:
  - Processing: 블루 + 시안 계열
  - Success: 그린 계열
  - Failure: 레드 + 코랄 계열
- 추천 카드는 주황 accent를 직접 채우지 말고, 중립 카드 위에 작은 색 포인트로만 사용한다.

## Interaction Principles

- 사용자는 분석을 시작한 뒤 "무엇을 기다리는지" 바로 이해해야 한다.
- 결과가 도착했을 때는 텍스트보다 공간 이동이 더 강한 피드백이 된다.
- 오류는 숨기지 말고 재시도 버튼까지 한 카드 안에서 해결한다.
- 캔버스와 사이드바는 서로 분리된 영역이 아니라, 작업 상태가 트리 변화로 이어지는 관계가 느껴져야 한다.

## Suggested Component Split

```text
DashboardPage
└─ DashboardWorkspace
   ├─ DashboardControlCenter
   │  ├─ AnalysisUrlForm
   │  ├─ SseStatusBadge
   │  ├─ ActivityQueue
   │  │  └─ ActivityQueueItem
   │  └─ RecommendationPanel
   │     └─ RecommendationCard
   └─ KnowledgeTreeCanvas
      ├─ CanvasToolbar
      ├─ TreeViewport
      ├─ TreeNode
      └─ NodeDetailPanel
```

## Suggested State Model

- `treeData`
- `selectedNode`
- `focusedNode`
- `pendingActivities[]`
- `failedActivities[]`
- `sseConnectionState`
- `recommendations`

### Activity Item Shape

```ts
type DashboardActivity = {
  taskId: string;
  sourceLabel: string;
  sourceUrl: string;
  createdAt: string;
  status: "processing" | "success" | "failed";
  message: string;
  relatedNode?: {
    category: string;
    topic: string;
    keyword: string;
  };
  errorCode?: string;
};
```

## Recommended Layout Rules

- Desktop:
  - `Control Center`: 360px ~ 420px
  - `Main Canvas`: 나머지 영역 전부
- Large desktop에서는 캔버스 최소 높이 `calc(100vh - topbar)` 기준 유지
- Mobile:
  - 캔버스 높이 `52vh` 이상 확보
  - URL 폼은 sticky 하지 않음
  - Activity list만 최대 높이와 내부 스크롤 허용

## Microcopy

- URL section helper:
  - `블로그 URL을 넣으면 지식을 분석해 트리에 연결합니다.`
- Processing:
  - `지식을 분석하고 트리를 구성 중입니다...`
- Success:
  - `분석 결과가 트리에 반영되었습니다.`
- Failure:
  - `분석에 실패했습니다. 다시 시도해 주세요.`
- SSE offline:
  - `실시간 연결이 끊어졌습니다. 자동으로 다시 연결하는 중입니다.`

## Implementation Order

1. 대시보드 전용 레이아웃과 정보 구조를 먼저 교체한다.
2. Activity List 상태 모델을 붙여 요청 직후 항목이 생성되게 한다.
3. SSE 연결 상태를 전역 또는 대시보드 범위 상태로 노출한다.
4. 성공 이벤트 수신 시 tree refetch + focus transition을 연결한다.
5. 실패 이벤트 수신 시 에러 카드와 재시도 액션을 붙인다.
6. 마지막에 모바일 축소 규칙과 모션을 다듬는다.

## Decision Notes

- 진행률 바 대신 고정 문구 + live indicator를 사용한다.
  - 현재 백엔드에서 중간 progress 이벤트가 없기 때문이다.
- 결과 반영 UX의 핵심은 토스트가 아니라 캔버스 포커싱이다.
  - 사용자는 "무엇이 바뀌었는지"를 공간적으로 이해하는 편이 더 빠르다.
- 실패 항목은 자동 제거하지 않는다.
  - 사용자가 재시도 가능성을 즉시 판단할 수 있어야 한다.
