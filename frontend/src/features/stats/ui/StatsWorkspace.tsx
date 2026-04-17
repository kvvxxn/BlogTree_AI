// 데모 데이터
const totalBlogs = 42;

const categoryData = [
  { name: "Backend", count: 25, percentage: 60, color: "#818cf8" }, // Indigo
  { name: "AI", count: 17, percentage: 40, color: "#2dd4bf" }, // Teal
];

const topTopics = [
  { rank: 1, name: "Spring", count: 12, percentage: 100 },
  { rank: 2, name: "RAG", count: 8, percentage: 67 },
  { rank: 3, name: "Database", count: 5, percentage: 42 },
];

const recentKeywords = [
  { name: "QueryDSL", time: "오늘" },
  { name: "Vector Index", time: "어제" },
  { name: "Reranking", time: "3일 전" },
  { name: "JPA", time: "5일 전" },
];

export function StatsWorkspace() {
  // 도넛 차트 계산
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

  return (
    <div className="min-h-screen bg-zinc-950 p-4 md:p-8 text-zinc-50 font-sans">
      <div className="max-w-5xl mx-auto space-y-6">
        
        {/* 헤더 섹션 */}
        <header className="mb-8">
          <h1 className="text-3xl font-extrabold tracking-tight">나의 지식 트리</h1>
          <p className="text-sm font-medium text-indigo-400 mt-1">커리어 목표: AI Backend Engineer</p>
        </header>

        {/* 0. 총 학습 블로그 - Hero */}
        <section className="bg-zinc-900 border border-zinc-800 rounded-2xl p-8 relative overflow-hidden flex items-center justify-between">
          <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl -mr-20 -mt-20 pointer-events-none" />
          <div className="relative z-10">
            <span className="text-xs font-bold tracking-wider text-zinc-400 uppercase">Total Knowledge</span>
            <div className="mt-2 flex items-baseline gap-3">
              <span className="text-6xl font-black text-white">{totalBlogs}</span>
              <span className="text-lg text-zinc-400">개의 블로그를 트리에 연결했어요!</span>
            </div>
          </div>
          <div className="hidden sm:block text-6xl opacity-20">🍃</div>
        </section>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* 1. 나의 학습 밸런스 - Donut Chart */}
          <section className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 flex flex-col">
            <span className="text-xs font-bold tracking-wider text-zinc-400 uppercase mb-4">Learning Balance</span>
            
            <div className="relative flex-1 flex flex-col items-center justify-center min-h-[250px]">
              <svg viewBox="0 0 100 100" className="w-48 h-48 drop-shadow-xl overflow-visible">
                {/* 배경 트랙 */}
                <circle cx="50" cy="50" r={radius} fill="none" stroke="#27272a" strokeWidth="12" />
                {/* 데이터 슬라이스 */}
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
                    className="transition-all duration-1000 ease-out"
                  />
                ))}
                {/* 외부 텍스트 라벨 (사용자 요청 반영) */}
                <text x="85" y="30" fill="#818cf8" fontSize="7" fontWeight="bold">Backend</text>
                <text x="5" y="80" fill="#2dd4bf" fontSize="7" fontWeight="bold">AI</text>
              </svg>
              
              {/* 도넛 중앙 텍스트 */}
              <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                <span className="text-3xl font-black">{totalBlogs}</span>
                <span className="text-[10px] text-zinc-400 font-medium">Total</span>
              </div>
            </div>

            {/* 깔끔한 범례 (Legend) */}
            <div className="mt-6 space-y-3 bg-zinc-950/50 p-4 rounded-xl border border-zinc-800/50">
              {categoryData.map((cat) => (
                <div key={cat.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="w-3 h-3 rounded-full" style={{ backgroundColor: cat.color }} />
                    <span className="font-medium text-zinc-200">{cat.name}</span>
                  </div>
                  <div className="flex items-center gap-4 text-sm">
                    <span style={{ color: cat.color }} className="font-bold">{cat.percentage}%</span>
                    <span className="text-zinc-500 w-8 text-right">{cat.count}개</span>
                  </div>
                </div>
              ))}
            </div>
            <p className="mt-4 text-xs text-zinc-500 text-center">
              현재 <strong className="text-indigo-400">Backend</strong> 카테고리 관련 블로그를 가장 많이 읽고 있어요!
            </p>
          </section>

          {/* 2. 가장 많이 학습한 토픽 TOP 3 */}
          <section className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 flex flex-col">
            <span className="text-xs font-bold tracking-wider text-zinc-400 uppercase mb-6">Top Topics</span>
            
            <div className="flex-1 flex flex-col justify-center space-y-6">
              {topTopics.map((topic) => (
                <div key={topic.rank} className="space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="text-lg">
                        {topic.rank === 1 ? "🥇" : topic.rank === 2 ? "🥈" : "🥉"}
                      </span>
                      <span className="font-semibold text-zinc-100">{topic.name}</span>
                    </div>
                    <span className="text-sm font-medium text-zinc-400">{topic.count}개</span>
                  </div>
                  {/* 프로그레스 바 추가 */}
                  <div className="h-2 w-full bg-zinc-800 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-indigo-500 rounded-full"
                      style={{ width: `${topic.percentage}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        {/* 3. 최근 획득한 키워드 */}
        <section className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6">
          <span className="text-xs font-bold tracking-wider text-zinc-400 uppercase mb-4 block">Recent Keywords</span>
          
          {/* 태그(배지) 형태의 리스트 */}
          <div className="flex flex-wrap gap-3 mt-4">
            {recentKeywords.map((kw) => (
              <div 
                key={kw.name} 
                className="flex items-center gap-2 bg-zinc-800 hover:bg-zinc-700 transition-colors px-4 py-2 rounded-full border border-zinc-700"
              >
                <span className="text-indigo-400 font-bold">#</span>
                <span className="text-sm font-medium text-zinc-200">{kw.name}</span>
                <span className="text-xs text-zinc-500 ml-1">{kw.time}</span>
              </div>
            ))}
          </div>
        </section>

      </div>
    </div>
  );
}
