import { useState } from "react";

type TreeKind = "category" | "topic" | "keyword";

type TreeNodeCard = {
  id: string;
  title: string;
  subtitle: string;
  kind: TreeKind;
  x: number;
  y: number;
  size: "lg" | "md" | "sm";
  summary?: string;
  sourceUrl?: string;
};

const treeNodes: TreeNodeCard[] = [
  { id: "backend", title: "Backend", subtitle: "Category", kind: "category", x: 10, y: 26, size: "lg" },
  { id: "ai", title: "AI", subtitle: "Category", kind: "category", x: 10, y: 68, size: "lg" },
  { id: "spring", title: "Spring", subtitle: "Topic", kind: "topic", x: 36, y: 22, size: "md" },
  { id: "rag", title: "RAG", subtitle: "Topic", kind: "topic", x: 38, y: 64, size: "md" },
  {
    id: "jpa",
    title: "JPA",
    subtitle: "Keyword",
    kind: "keyword",
    x: 66,
    y: 16,
    size: "sm",
    summary: "Spring 기반 데이터 접근에서 ORM 매핑, 영속성 컨텍스트, 트랜잭션 경계를 이해하는 핵심 키워드.",
    sourceUrl: "https://spring.io/projects/spring-data-jpa",
  },
  {
    id: "querydsl",
    title: "QueryDSL",
    subtitle: "Keyword",
    kind: "keyword",
    x: 70,
    y: 34,
    size: "sm",
    summary: "복잡한 동적 쿼리를 타입 안정성 있게 구성할 때 사용하는 도구로 JPA 학습 다음 단계에 자주 연결된다.",
    sourceUrl: "https://querydsl.com/",
  },
  {
    id: "vector-index",
    title: "Vector Index",
    subtitle: "Keyword",
    kind: "keyword",
    x: 67,
    y: 58,
    size: "sm",
    summary: "RAG 시스템에서 임베딩 기반 검색 품질과 성능을 좌우하는 인덱싱 전략에 대한 핵심 개념.",
    sourceUrl: "https://www.pinecone.io/learn/vector-indexes/",
  },
  {
    id: "reranking",
    title: "Reranking",
    subtitle: "Keyword",
    kind: "keyword",
    x: 73,
    y: 77,
    size: "sm",
    summary: "1차 검색 결과를 다시 정렬해 문맥 적합도를 높이는 단계로, 검색 정확도 개선에 직접 연결된다.",
    sourceUrl: "https://www.pinecone.io/learn/series/rag/rerankers/",
  },
];

const treeLinks = [
  { from: { x: 18, y: 31 }, to: { x: 43, y: 27 } },
  { from: { x: 18, y: 72 }, to: { x: 45, y: 68 } },
  { from: { x: 44, y: 25 }, to: { x: 71, y: 20 } },
  { from: { x: 46, y: 28 }, to: { x: 75, y: 38 } },
  { from: { x: 46, y: 67 }, to: { x: 72, y: 62 } },
  { from: { x: 47, y: 69 }, to: { x: 78, y: 80 } },
];

function getNodeClassName(node: TreeNodeCard) {
  const clickableClass = node.kind === "keyword" ? " tree-node-card--interactive" : "";
  return `tree-node-card tree-node-card--${node.kind} tree-node-card--${node.size}${clickableClass}`;
}

export function DashboardOverview() {
  const [selectedKeywordId, setSelectedKeywordId] = useState<string | null>("vector-index");
  const selectedKeyword =
    treeNodes.find((node) => node.id === selectedKeywordId && node.kind === "keyword") ?? null;

  return (
    <>
      <section className="dashboard-canvas dashboard-canvas--full">
        <section className="card dashboard-canvas__hero">
          <div className="dashboard-canvas__heading">
            <div>
              <span className="section-label">Dashboard</span>
              <h1>Category &gt; Topic &gt; Keyword 구조로 지식을 탐색한다.</h1>
              <p>
                키워드를 클릭하면 요약본과 원본 URL을 지식 카드 모달로 바로 확인할 수 있다.
              </p>
            </div>
          </div>
        </section>

        <section className="card dashboard-tree-shell">
          <div className="dashboard-tree-shell__header">
            <div>
              <span className="section-label">Knowledge Tree</span>
              <h2>Category &gt; Topic &gt; Keyword</h2>
            </div>
          </div>

          <div className="tree-viewport tree-viewport--simple">
            <svg
              className="tree-viewport__links"
              viewBox="0 0 100 100"
              preserveAspectRatio="none"
              aria-hidden="true"
            >
              {treeLinks.map((link, index) => (
                <line
                  key={`${link.from.x}-${link.to.x}-${index}`}
                  x1={link.from.x}
                  y1={link.from.y}
                  x2={link.to.x}
                  y2={link.to.y}
                />
              ))}
            </svg>

            {treeNodes.map((node) => {
              const isSelected = selectedKeyword?.id === node.id;
              const isKeyword = node.kind === "keyword";

              return (
                <button
                  key={node.id}
                  className={`${getNodeClassName(node)}${isSelected ? " tree-node-card--selected" : ""}`}
                  style={{ left: `${node.x}%`, top: `${node.y}%` }}
                  type="button"
                  onClick={() => {
                    if (isKeyword) {
                      setSelectedKeywordId(node.id);
                    }
                  }}
                  aria-pressed={isSelected}
                >
                  <span>{node.subtitle}</span>
                  <strong>{node.title}</strong>
                </button>
              );
            })}
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
