import { useEffect, useMemo, useState } from "react";
import { getStats } from "@/features/stats/api";
import { ApiError } from "@/shared/api/http";
import { logger } from "@/shared/lib/logger";
import type { StatsResponse } from "@/shared/types/api";
import "./StatsWorkspace.css";

type LoadStatus = "loading" | "success" | "error";

type CategoryView = {
  name: string;
  count: number;
  percentage: number;
  color: string;
};

type TopicView = {
  rank: number;
  name: string;
  count: number;
  percentage: number;
};

type KeywordView = {
  name: string;
  time: string;
  readAt: string;
};

const categoryColors = ["#818cf8", "#2dd4bf", "#f472b6", "#fbbf24", "#60a5fa"];

function formatPercentage(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

function formatRelativeDate(value: string) {
  const readAt = new Date(value);

  if (Number.isNaN(readAt.getTime())) {
    return "최근";
  }

  const today = new Date();
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
  const readAtStart = new Date(readAt.getFullYear(), readAt.getMonth(), readAt.getDate()).getTime();
  const diffDays = Math.floor((todayStart - readAtStart) / 86_400_000);

  if (diffDays <= 0) {
    return "오늘";
  }

  if (diffDays === 1) {
    return "어제";
  }

  if (diffDays < 7) {
    return `${diffDays}일 전`;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "short",
    day: "numeric",
  }).format(readAt);
}

function toCategoryView(stats: StatsResponse): CategoryView[] {
  return stats.categoryStats.map((category, index) => ({
    name: category.category,
    count: category.count,
    percentage: category.percentage,
    color: categoryColors[index % categoryColors.length],
  }));
}

function toTopicView(stats: StatsResponse): TopicView[] {
  const maxCount = Math.max(...stats.topTopics.map((topic) => topic.count), 0);

  return stats.topTopics.map((topic, index) => ({
    rank: index + 1,
    name: topic.topic,
    count: topic.count,
    percentage: maxCount === 0 ? 0 : Math.round((topic.count / maxCount) * 100),
  }));
}

function toKeywordView(stats: StatsResponse): KeywordView[] {
  return stats.recentKeywords.map((keyword) => ({
    name: keyword.keyword,
    time: formatRelativeDate(keyword.readAt),
    readAt: keyword.readAt,
  }));
}

export function StatsWorkspace() {
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [status, setStatus] = useState<LoadStatus>("loading");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadStats() {
      try {
        logger.info("stats", "통계 조회를 시작합니다.");
        setStatus("loading");
        setErrorMessage(null);
        const response = await getStats();

        if (cancelled) {
          return;
        }

        logger.info("stats", "통계 조회에 성공했습니다.", {
          totalReadCount: response.totalReadCount,
        });
        setStats(response);
        setStatus("success");
      } catch (error) {
        if (cancelled) {
          return;
        }

        logger.error("stats", "통계 조회에 실패했습니다.", error);
        setStatus("error");
        setErrorMessage(
          error instanceof ApiError
            ? error.message
            : "학습 통계를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.",
        );
      }
    }

    void loadStats();

    return () => {
      cancelled = true;
    };
  }, [reloadToken]);

  const totalBlogs = stats?.totalReadCount ?? 0;
  const categoryData = useMemo(() => (stats ? toCategoryView(stats) : []), [stats]);
  const topTopics = useMemo(() => (stats ? toTopicView(stats) : []), [stats]);
  const recentKeywords = useMemo(() => (stats ? toKeywordView(stats) : []), [stats]);
  const topCategory = categoryData[0];

  const radius = 35;
  const circumference = 2 * Math.PI * radius;
  let cumulativeOffset = 0;

  const segments = categoryData.map((cat) => {
    const segmentLength = (cat.percentage / 100) * circumference;
    const offset = cumulativeOffset;
    cumulativeOffset += segmentLength;
    return {
      ...cat,
      dasharray: `${segmentLength} ${circumference}`,
      dashoffset: -offset,
    };
  });

  function handleRetry() {
    setStatus("loading");
    setErrorMessage(null);
    setStats(null);
    setReloadToken((current) => current + 1);
  }

  return (
    <div className="workspace-wrapper">
      <div className="workspace-container">
        <header className="workspace-header">
          <h1 className="header-career-goal">{stats?.careerGoal || "나의 지식 트리"}</h1>
        </header>

        {status === "loading" ? (
          <section className="stats-card stats-status-card">
            <strong>학습 통계를 불러오는 중입니다</strong>
            <span>연결한 블로그와 최근 키워드를 정리하고 있어요.</span>
          </section>
        ) : null}

        {status === "error" ? (
          <section className="stats-card stats-status-card stats-status-card--error">
            <strong>학습 통계를 불러오지 못했습니다</strong>
            <span>{errorMessage ?? "잠시 후 다시 시도해 주세요."}</span>
            <button className="stats-retry-button" type="button" onClick={handleRetry}>
              다시 시도
            </button>
          </section>
        ) : null}

        {status === "success" ? (
          <>
            <section className="stats-card hero-card">
              <div className="hero-glow" />
              <div className="hero-content">
                <span className="section-label">Total Knowledge</span>
                <div className="hero-number-group">
                  <span className="hero-number">{totalBlogs}</span>
                  <span className="hero-text">개의 블로그를 트리에 연결했어요!</span>
                </div>
              </div>
              <div className="hero-icon">🍃</div>
            </section>

            <div className="stats-grid">
              <section className="stats-card donut-section">
                <span className="section-label">Learning Balance</span>

                <div className="donut-container">
                  <svg viewBox="0 0 100 100" className="donut-svg" aria-label="카테고리별 학습 비율">
                    <circle cx="50" cy="50" r={radius} className="donut-track" />
                    {segments.map((seg) => (
                      <circle
                        key={seg.name}
                        cx="50"
                        cy="50"
                        r={radius}
                        fill="none"
                        stroke={seg.color}
                        strokeWidth="12"
                        strokeLinecap="round"
                        strokeDasharray={seg.dasharray}
                        strokeDashoffset={seg.dashoffset}
                        transform="rotate(-90 50 50)"
                        className="donut-slice"
                      />
                    ))}
                  </svg>

                  <div className="donut-center">
                    <span className="donut-center-total">{totalBlogs}</span>
                    <span className="donut-center-label">Total</span>
                  </div>
                </div>

                {categoryData.length > 0 ? (
                  <>
                    <div className="donut-legend">
                      {categoryData.map((cat) => (
                        <div key={cat.name} className="legend-item">
                          <div className="legend-info">
                            <span className="legend-dot" style={{ backgroundColor: cat.color }} />
                            <span className="legend-name">{cat.name}</span>
                          </div>
                          <div className="legend-stats">
                            <span className="legend-percent" style={{ color: cat.color }}>
                              {formatPercentage(cat.percentage)}%
                            </span>
                            <span className="legend-count">{cat.count}개</span>
                          </div>
                        </div>
                      ))}
                    </div>
                    <p className="donut-insight">
                      현재 <strong className="highlight-indigo">{topCategory.name}</strong> 카테고리 관련 블로그를 가장 많이 읽고 있어요!
                    </p>
                  </>
                ) : (
                  <p className="stats-empty-text">아직 카테고리 통계가 없습니다.</p>
                )}
              </section>

              <section className="stats-card ranking-section">
                <span className="section-label">Top Topics</span>

                {topTopics.length > 0 ? (
                  <div className="ranking-list">
                    {topTopics.map((topic) => (
                      <div key={topic.name} className="ranking-item">
                        <div className="ranking-info">
                          <div className="ranking-name-group">
                            <span className="ranking-medal">
                              {topic.rank === 1 ? "🥇" : topic.rank === 2 ? "🥈" : "🥉"}
                            </span>
                            <span className="ranking-name">{topic.name}</span>
                          </div>
                          <span className="ranking-count">{topic.count}개</span>
                        </div>
                        <div className="ranking-progress-track">
                          <div
                            className="ranking-progress-fill"
                            style={{ width: `${topic.percentage}%` }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="stats-empty-text">아직 토픽 통계가 없습니다.</p>
                )}
              </section>
            </div>

            <section className="stats-card keywords-section">
              <span className="section-label">Recent Keywords</span>

              {recentKeywords.length > 0 ? (
                <div className="keyword-list">
                  {recentKeywords.map((kw) => (
                    <div key={`${kw.name}-${kw.readAt}`} className="keyword-tag">
                      <span className="keyword-hash">#</span>
                      <span className="keyword-name">{kw.name}</span>
                      <span className="keyword-time">{kw.time}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="stats-empty-text">최근 획득한 키워드가 없습니다.</p>
              )}
            </section>
          </>
        ) : null}
      </div>
    </div>
  );
}
