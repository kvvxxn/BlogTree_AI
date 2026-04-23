import { useEffect, useMemo, useState } from "react";
import { getKnowledgeTree } from "@/features/tree/api/tree.api";
import type { KnowledgeTree } from "@/shared/types/api";
import { ApiError } from "@/shared/api/http";
import { logger } from "@/shared/lib/logger";

type TreeKind = "goal" | "category" | "topic" | "keyword";

type TreeBranch = {
  category: string;
  topics: {
    topic: string;
    keywords: string[];
  }[];
};

type SelectedKeyword = {
  id: string;
  category: string;
  topic: string;
  title: string;
};

type LoadStatus = "loading" | "success" | "error";

function getNodeClassName(kind: TreeKind, isSelected: boolean = false) {
  const baseClass = `tree-node-card tree-node-card--${kind}`;
  const interactiveClass = kind === "keyword" ? " tree-node-card--interactive" : "";
  const selectedClass = isSelected ? " tree-node-card--selected" : "";
  return `${baseClass}${interactiveClass}${selectedClass}`;
}

function createKeywordId(category: string, topic: string, keyword: string) {
  return `${category}::${topic}::${keyword}`;
}

function toBranches(tree: KnowledgeTree): TreeBranch[] {
  return Object.entries(tree).map(([category, topics]) => ({
    category,
    topics: Object.entries(topics).map(([topic, keywords]) => ({
      topic,
      keywords,
    })),
  }));
}

export function DashboardOverview() {
  const [tree, setTree] = useState<KnowledgeTree>({});
  const [status, setStatus] = useState<LoadStatus>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [selectedKeywordId, setSelectedKeywordId] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadTree() {
      try {
        logger.info("tree", "지식 트리 조회를 시작합니다.");
        setStatus("loading");
        setErrorMessage(null);
        const response = await getKnowledgeTree();

        if (cancelled) {
          return;
        }

        logger.info("tree", "지식 트리 조회에 성공했습니다.", {
          categoryCount: Object.keys(response).length,
        });
        setTree(response);
        setStatus("success");
      } catch (error) {
        if (cancelled) {
          return;
        }

        logger.error("tree", "지식 트리 조회에 실패했습니다.", error);
        setStatus("error");
        setErrorMessage(
          error instanceof ApiError
            ? error.message
            : "지식 트리를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.",
        );
      }
    }

    void loadTree();

    return () => {
      cancelled = true;
    };
  }, [reloadToken]);

  const branches = useMemo(() => toBranches(tree), [tree]);

  const selectedKeyword = useMemo<SelectedKeyword | null>(() => {
    if (!selectedKeywordId) {
      return null;
    }

    for (const branch of branches) {
      for (const topic of branch.topics) {
        for (const keyword of topic.keywords) {
          const id = createKeywordId(branch.category, topic.topic, keyword);
          if (id === selectedKeywordId) {
            return {
              id,
              category: branch.category,
              topic: topic.topic,
              title: keyword,
            };
          }
        }
      }
    }

    return null;
  }, [branches, selectedKeywordId]);

  const topicCount = branches.reduce((count, branch) => count + branch.topics.length, 0);
  const keywordCount = branches.reduce(
    (count, branch) =>
      count + branch.topics.reduce((topicTotal, topic) => topicTotal + topic.keywords.length, 0),
    0,
  );

  function handleRetry() {
    setStatus("loading");
    setErrorMessage(null);
    setTree({});
    setSelectedKeywordId(null);
    setReloadToken((current) => current + 1);
  }

  return (
    <>
      <section className="dashboard-canvas dashboard-canvas--full">
        <section className="card dashboard-tree-shell">
          <div className="dashboard-tree-shell__header">
            <div>
              <span className="section-label">Knowledge Tree</span>
              <h2>카테고리부터 키워드까지 학습 지도를 탐색하세요</h2>
              <div className="tree-legend">
                <span className="tree-legend__item tree-legend__item--category">카테고리</span>
                <span className="tree-legend__arrow">→</span>
                <span className="tree-legend__item tree-legend__item--topic">토픽</span>
                <span className="tree-legend__arrow">→</span>
                <span className="tree-legend__item tree-legend__item--keyword">키워드</span>
              </div>
            </div>
            <div className="dashboard-tree-shell__stats">
              <div className="dashboard-mini-stat">
                <span>Categories</span>
                <strong>{branches.length}</strong>
              </div>
              <div className="dashboard-mini-stat">
                <span>Topics</span>
                <strong>{topicCount}</strong>
              </div>
              <div className="dashboard-mini-stat">
                <span>Keywords</span>
                <strong>{keywordCount}</strong>
              </div>
            </div>
          </div>

          {status === "loading" ? (
            <div className="dashboard-inline-status">
              <strong>트리를 불러오는 중입니다</strong>
              <span>저장된 카테고리, 토픽, 키워드를 정리하고 있습니다.</span>
            </div>
          ) : null}

          {status === "error" ? (
            <div className="dashboard-inline-status dashboard-inline-status--error">
              <strong>트리를 불러오지 못했습니다</strong>
              <span>{errorMessage ?? "잠시 후 다시 시도해 주세요."}</span>
              <button className="button button--primary" type="button" onClick={handleRetry}>
                다시 시도
              </button>
            </div>
          ) : null}

          {status === "success" && branches.length === 0 ? (
            <div className="dashboard-inline-status">
              <strong>아직 생성된 지식 트리가 없습니다</strong>
              <span>블로그를 요약하면 카테고리와 키워드가 이곳에 쌓입니다.</span>
            </div>
          ) : null}

          {status === "success" && branches.length > 0 ? (
            <div className="tree-viewport tree-viewport--branch-grid">
              <div className="tree-branch-list">
                <div className="tree-goal-column">
                  <div className="tree-node-card tree-node-card--goal tree-node-card--center">
                    <span>Knowledge Root</span>
                    <strong>My Learning Tree</strong>
                  </div>
                </div>

                <div className="tree-branch-grid">
                  {branches.map((branch) => (
                    <article key={branch.category} className="tree-branch-card">
                      <div className="tree-branch-card__category">
                        <div className={getNodeClassName("category")}>
                          <span>Category</span>
                          <strong>{branch.category}</strong>
                        </div>
                      </div>

                      <div className="tree-topic-list">
                        {branch.topics.map((topic) => (
                          <section
                            key={`${branch.category}-${topic.topic}`}
                            className="tree-topic-card"
                          >
                            <div className={getNodeClassName("topic")}>
                              <span>Topic</span>
                              <strong>{topic.topic}</strong>
                            </div>

                            <div className="tree-keyword-list">
                              {topic.keywords.map((keyword) => {
                                const keywordId = createKeywordId(
                                  branch.category,
                                  topic.topic,
                                  keyword,
                                );

                                return (
                                  <button
                                    key={keywordId}
                                    className={getNodeClassName(
                                      "keyword",
                                      selectedKeywordId === keywordId,
                                    )}
                                    type="button"
                                    onClick={() => setSelectedKeywordId(keywordId)}
                                    aria-pressed={selectedKeywordId === keywordId}
                                  >
                                    <span>Keyword</span>
                                    <strong>{keyword}</strong>
                                  </button>
                                );
                              })}
                            </div>
                          </section>
                        ))}
                      </div>
                    </article>
                  ))}
                </div>
              </div>
            </div>
          ) : null}
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
                <h2 id="knowledge-card-title" className="knowledge-modal__title-path">
                  <span className="knowledge-title__category">{selectedKeyword.category}</span>
                  <span className="knowledge-title__arrow">→</span>
                  <span className="knowledge-title__topic">{selectedKeyword.topic}</span>
                  <span className="knowledge-title__arrow">→</span>
                  <span className="knowledge-title__keyword">{selectedKeyword.title}</span>
                </h2>
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
                <strong>현재 표시 가능한 정보</strong>
                <p>
                  이 화면은 `GET /api/tree` 응답으로 만든 구조 트리입니다. 현재 백엔드는
                  category, topic, keyword 이름만 내려주므로 상세 요약과 원본 URL은 포함되지
                  않습니다.
                </p>
              </div>
              <div className="knowledge-modal__section">
                <strong>선택된 키워드</strong>
                <p>{selectedKeyword.title}</p>
              </div>
            </div>
          </section>
        </div>
      ) : null}
    </>
  );
}
