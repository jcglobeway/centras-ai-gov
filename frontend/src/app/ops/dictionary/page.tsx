"use client";

import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { PageGuide } from "@/components/ui/PageGuide";

const MOCK_SYNONYMS = [
  { id: 1, terms: ['복지', '사회보장', '급여'],   createdAt: '2026-03-01' },
  { id: 2, terms: ['민원', '신청', '접수'],        createdAt: '2026-03-05' },
  { id: 3, terms: ['문의', '질문', '질의'],        createdAt: '2026-03-10' },
];

const MOCK_FORBIDDEN = [
  { word: '욕설1',    action: '차단', count: 3 },
  { word: '비방표현', action: '경고', count: 1 },
];

const MockBadge = () => (
  <span className="text-[10px] font-mono text-warning bg-warning/10 border border-warning/20 rounded px-2 py-0.5">
    목업 데이터
  </span>
);

const actionBadge = (action: string) => {
  const style = action === '차단'
    ? 'bg-error/10 text-error border border-error/30'
    : 'bg-warning/10 text-warning border border-warning/30';
  return (
    <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded-full ${style}`}>
      {action}
    </span>
  );
};

export default function DictionaryPage() {
  return (
    <div className="space-y-6">
      <PageGuide
        description="검색 품질 개선을 위한 동의어 그룹과 금칙어를 관리하는 화면입니다."
        tips={[
          "시민이 자주 쓰는 비공식 표현(속어, 줄임말)을 공식 용어와 동의어로 등록하면 검색 정확도가 높아집니다.",
          "금칙어 차단 로그를 주기적으로 확인해 새로운 부적절 표현을 차단 목록에 추가하세요.",
          "동의어 그룹은 CSV로 일괄 업로드할 수 있습니다.",
        ]}
      />
      <div className="flex items-center justify-between">
        <h2 className="text-text-primary font-semibold text-lg">동의어/금칙어 관리</h2>
        <MockBadge />
      </div>

      {/* 동의어 그룹 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">동의어 그룹</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-bg-border">
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted w-16">ID</th>
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">동의어 그룹</th>
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted w-28">생성일</th>
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted w-24">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-bg-border">
              {MOCK_SYNONYMS.map((row) => (
                <tr key={row.id}>
                  <td className="py-2.5 font-mono text-[11px] text-text-muted">{row.id}</td>
                  <td className="py-2.5">
                    <div className="flex flex-wrap gap-1">
                      {row.terms.map((term) => (
                        <span
                          key={term}
                          className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-accent/10 text-accent border border-accent/30"
                        >
                          {term}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="py-2.5 text-xs text-text-muted">{row.createdAt}</td>
                  <td className="py-2.5">
                    <div className="flex gap-2">
                      <button
                        disabled
                        className="text-[11px] text-accent opacity-50 cursor-not-allowed"
                      >
                        편집
                      </button>
                      <button
                        disabled
                        className="text-[11px] text-error opacity-50 cursor-not-allowed"
                      >
                        삭제
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="mt-3 pt-3 border-t border-bg-border">
            <button
              disabled
              className="px-4 py-2 rounded-lg bg-bg-elevated border border-bg-border text-text-secondary text-sm font-medium opacity-50 cursor-not-allowed"
            >
              + 새 그룹 추가
            </button>
          </div>
        </div>
      </Card>

      {/* 금칙어 목록 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">금칙어 목록</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-bg-border">
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">금칙어</th>
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">처리 방식</th>
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">차단 건수</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-bg-border">
              {MOCK_FORBIDDEN.map((row) => (
                <tr key={row.word}>
                  <td className="py-2.5 text-sm text-text-primary">{row.word}</td>
                  <td className="py-2.5">{actionBadge(row.action)}</td>
                  <td className="py-2.5 font-mono text-sm text-text-secondary">{row.count}건</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {/* 차단 로그 요약 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">차단 로그 요약</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <div className="grid grid-cols-3 gap-4">
            <div className="bg-bg-prominent rounded-lg p-4 text-center">
              <p className="font-mono text-2xl font-bold text-error">4</p>
              <p className="text-[10px] text-text-muted mt-1">이번 달 차단</p>
            </div>
            <div className="bg-bg-prominent rounded-lg p-4 text-center">
              <p className="font-mono text-2xl font-bold text-warning">1</p>
              <p className="text-[10px] text-text-muted mt-1">이번 달 경고</p>
            </div>
            <div className="bg-bg-prominent rounded-lg p-4 text-center">
              <p className="font-mono text-2xl font-bold text-text-primary">2</p>
              <p className="text-[10px] text-text-muted mt-1">등록 금칙어 수</p>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
