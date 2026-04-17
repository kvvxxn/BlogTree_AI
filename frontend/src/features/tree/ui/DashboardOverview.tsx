import { useState } from "react";

type TreeKind = "goal" | "category" | "topic" | "keyword";

type TreeNodeCard = {
  id: string;
  title: string;
  subtitle: string;
  kind: TreeKind;
  summary?: string;
  sourceUrl?: string;
};

// 왼쪽 Career Goal
const careerGoal: TreeNodeCard = {
  id: "career-goal",
  title: "AI Backend Engineer",
  subtitle: "Career Goal",
  kind: "goal",
};

// 상단 브랜치 (Backend 계열)
const upperBranch = {
  category: { id: "backend", title: "Backend", subtitle: "Category", kind: "category" as TreeKind },
  topics: [
    { id: "spring", title: "Spring", subtitle: "Topic", kind: "topic" as TreeKind },
  ],
  keywords: [
    {
      id: "jpa",
      title: "JPA",
      subtitle: "Keyword",
      kind: "keyword" as TreeKind,
      summary: "Spring 기반 데이터 접근에서 ORM 매핑, 영속성 컨텍스트, 트랜잭션 경계를 이해하는 핵심 키워드.",
      sourceUrl: "https://spring.io/projects/spring-data-jpa",
    },
    {
      id: "querydsl",
      title: "QueryDSL",
      subtitle: "Keyword",
      kind: "keyword" as TreeKind,
      summary: "복잡한 동적 쿼리를 타입 안정성 있게 구성할 때 사용하는 도구로 JPA 학습 다음 단계에 자주 연결된다.",
      sourceUrl: "https://querydsl.com/",
    },
  ],
};

// 하단 브랜치 (AI 계열)
const lowerBranch = {
  category: { id: "ai", title: "AI", subtitle: "Category", kind: "category" as TreeKind },
  topics: [
    { id: "rag", title: "RAG", subtitle: "Topic", kind: "topic" as TreeKind },
  ],
  keywords: [
    {
      id: "vector-index",
      title: "Vector Index",
      subtitle: "Keyword",
      kind: "keyword" as TreeKind,
      summary: "RAG 시스템에서 임베딩 기반 검색 품질과 성능을 좌우하는 인덱싱 전략에 대한 핵심 개념.",
      sourceUrl: "https://www.pinecone.io/learn/vector-indexes/",
    },
    {
      id: "reranking",
      title: "Reranking",
      subtitle: "Keyword",
      kind: "keyword" as TreeKind,
      summary: "1차 검색 결과를 다시 정렬해 문맥 적합도를 높이는 단계로, 검색 정확도 개선에 직접 연결된다.",
      sourceUrl: "https://www.pinecone.io/learn/series/rag/rerankers/",
    },
  ],
};

function getNodeClassName(kind: TreeKind, isSelected: boolean = false) {
  const baseClass = `tree-node-card tree-node-card--${kind}`;
  const interactiveClass = kind === "keyword" ? " tree-node-card--interactive" : "";
  const selectedClass = isSelected ? " tree-node-card--selected" : "";
  return `${baseClass}${interactiveClass}${selectedClass}`;
}

export function DashboardOverview() {
  const [selectedKeywordId, setSelectedKeywordId] = useState<string | null>(null);
  
  const allKeywords = [...upperBranch.keywords, ...lowerBranch.keywords];
  const selectedKeyword = allKeywords.find((k) => k.id === selectedKeywordId) ?? null;

  // 선택된 키워드의 경로 찾기 (Category -> Topic -> Keyword)
  function getKeywordPath(keywordId: string | null) {
    if (!keywordId) return null;
    
    if (upperBranch.keywords.some((k) => k.id === keywordId)) {
      return {
        category: upperBranch.category.title,
        topic: upperBranch.topics[0].title,
      };
    }
    if (lowerBranch.keywords.some((k) => k.id === keywordId)) {
      return {
        category: lowerBranch.category.title,
        topic: lowerBranch.topics[0].title,
      };
    }
    return null;
  }

  const keywordPath = getKeywordPath(selectedKeywordId);

  // 수평 트리 레이아웃: Goal(왼쪽) -> Category(중앙 상하) -> Topic -> Keyword
  // X 좌표: Goal 8%, Category 28%, Topic 52%, Keyword 78%
  // Y 좌표: 상단 브랜치 25%, 하단 브랜치 75%

  return (
    <>
      <section className="dashboard-canvas dashboard-canvas--full">
        <section className="card dashboard-tree-shell">
          <div className="dashboard-tree-shell__header">
            <div>
              <span className="section-label">Knowledge Tree</span>
              <div className="tree-legend">
                <span className="tree-legend__item tree-legend__item--category">카테고리</span>
                <span className="tree-legend__arrow">→</span>
                <span className="tree-legend__item tree-legend__item--topic">토픽</span>
                <span className="tree-legend__arrow">→</span>
                <span className="tree-legend__item tree-legend__item--keyword">키워드</span>
              </div>
            </div>
          </div>

          <div className="tree-viewport tree-viewport--horizontal">
            <svg
              className="tree-viewport__links"
              viewBox="0 0 100 100"
              preserveAspectRatio="none"
              aria-hidden="true"
            >
              {/* Goal -> 수평 트렁크 -> 상하 분기 -> Categories */}
              <line x1="16" y1="50" x2="22" y2="50" className="tree-link tree-link--goal" />
              <line x1="22" y1="28" x2="22" y2="72" className="tree-link tree-link--goal" />
              <line x1="22" y1="28" x2="28" y2="28" className="tree-link tree-link--goal" />
              <line x1="22" y1="72" x2="28" y2="72" className="tree-link tree-link--goal" />
              
              {/* Upper: Category -> Topic */}
              <line x1="36" y1="28" x2="50" y2="28" className="tree-link tree-link--category" />
              
              {/* Upper: Topic -> Keywords */}
              <line x1="58" y1="28" x2="68" y2="28" className="tree-link tree-link--topic" />
              <line x1="68" y1="28" x2="68" y2="18" className="tree-link tree-link--topic" />
              <line x1="68" y1="28" x2="68" y2="38" className="tree-link tree-link--topic" />
              <line x1="68" y1="18" x2="74" y2="18" className="tree-link tree-link--topic" />
              <line x1="68" y1="38" x2="74" y2="38" className="tree-link tree-link--topic" />
              
              {/* Lower: Category -> Topic */}
              <line x1="36" y1="72" x2="50" y2="72" className="tree-link tree-link--category" />
              
              {/* Lower: Topic -> Keywords */}
              <line x1="58" y1="72" x2="68" y2="72" className="tree-link tree-link--topic" />
              <line x1="68" y1="72" x2="68" y2="62" className="tree-link tree-link--topic" />
              <line x1="68" y1="72" x2="68" y2="82" className="tree-link tree-link--topic" />
              <line x1="68" y1="62" x2="74" y2="62" className="tree-link tree-link--topic" />
              <line x1="68" y1="82" x2="74" y2="82" className="tree-link tree-link--topic" />
            </svg>

            {/* Left: Career Goal */}
            <div
              className="tree-node-card tree-node-card--goal"
              style={{ left: "8%", top: "50%", transform: "translateY(-50%)" }}
            >
              <span>{careerGoal.subtitle}</span>
              <strong>{careerGoal.title}</strong>
            </div>

            {/* Upper Branch - Category */}
            <button
              className={getNodeClassName("category")}
              style={{ left: "32%", top: "28%", transform: "translate(-50%, -50%)" }}
              type="button"
            >
              <span>{upperBranch.category.subtitle}</span>
              <strong>{upperBranch.category.title}</strong>
            </button>

            {/* Upper Branch - Topic */}
            <button
              className={getNodeClassName("topic")}
              style={{ left: "54%", top: "28%", transform: "translate(-50%, -50%)" }}
              type="button"
            >
              <span>{upperBranch.topics[0].subtitle}</span>
              <strong>{upperBranch.topics[0].title}</strong>
            </button>

            {/* Upper Branch - Keywords */}
            {upperBranch.keywords.map((keyword, index) => (
              <button
                key={keyword.id}
                className={getNodeClassName("keyword", selectedKeywordId === keyword.id)}
                style={{ 
                  left: "80%", 
                  top: `${18 + index * 20}%`, 
                  transform: "translate(-50%, -50%)" 
                }}
                type="button"
                onClick={() => setSelectedKeywordId(keyword.id)}
                aria-pressed={selectedKeywordId === keyword.id}
              >
                <span>{keyword.subtitle}</span>
                <strong>{keyword.title}</strong>
              </button>
            ))}

            {/* Lower Branch - Category */}
            <button
              className={getNodeClassName("category")}
              style={{ left: "32%", top: "72%", transform: "translate(-50%, -50%)" }}
              type="button"
            >
              <span>{lowerBranch.category.subtitle}</span>
              <strong>{lowerBranch.category.title}</strong>
            </button>

            {/* Lower Branch - Topic */}
            <button
              className={getNodeClassName("topic")}
              style={{ left: "54%", top: "72%", transform: "translate(-50%, -50%)" }}
              type="button"
            >
              <span>{lowerBranch.topics[0].subtitle}</span>
              <strong>{lowerBranch.topics[0].title}</strong>
            </button>

            {/* Lower Branch - Keywords */}
            {lowerBranch.keywords.map((keyword, index) => (
              <button
                key={keyword.id}
                className={getNodeClassName("keyword", selectedKeywordId === keyword.id)}
                style={{ 
                  left: "80%", 
                  top: `${62 + index * 20}%`, 
                  transform: "translate(-50%, -50%)" 
                }}
                type="button"
                onClick={() => setSelectedKeywordId(keyword.id)}
                aria-pressed={selectedKeywordId === keyword.id}
              >
                <span>{keyword.subtitle}</span>
                <strong>{keyword.title}</strong>
              </button>
            ))}
          </div>
        </section>
      </section>

      {selectedKeyword ? (
        <div
          className="knowledge-modal-backdrop"
          role="presentation"
          onClick={() => setSelectedKeywordId(null)}
        >
          <section
            className="card knowledge-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="knowledge-card-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="knowledge-modal__header">
              <div>
                <span className="section-label">Knowledge Card</span>
                {keywordPath && (
                  <div className="knowledge-modal__path">
                    <span className="knowledge-path__item knowledge-path__item--category">{keywordPath.category}</span>
                    <span className="knowledge-path__arrow">→</span>
                    <span className="knowledge-path__item knowledge-path__item--topic">{keywordPath.topic}</span>
                    <span className="knowledge-path__arrow">→</span>
                    <span className="knowledge-path__item knowledge-path__item--keyword">{selectedKeyword.title}</span>
                  </div>
                )}
                <h2 id="knowledge-card-title">{selectedKeyword.title}</h2>
              </div>
              <button
                className="button button--ghost"
                type="button"
                onClick={() => setSelectedKeywordId(null)}
              >
                닫기
              </button>
            </div>

            <div className="knowledge-modal__body">
              <div className="knowledge-modal__section">
                <strong>요약본</strong>
                <p>{selectedKeyword.summary}</p>
              </div>
              <div className="knowledge-modal__section">
                <strong>원본 URL</strong>
                <a href={selectedKeyword.sourceUrl} target="_blank" rel="noreferrer">
                  {selectedKeyword.sourceUrl}
                </a>
              </div>
            </div>
          </section>
        </div>
      ) : null}
    </>
  );
}
