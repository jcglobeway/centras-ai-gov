"use client";

import { useState } from "react";
import useSWR from "swr";
import { fetcher } from "@/lib/api";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageGuide } from "@/components/ui/PageGuide";

interface IngestionJob {
  jobId: string;
  crawlSourceId: string;
  jobStage: string;
  createdAt: string;
}

interface PagedResponse<T> {
  items: T[];
  totalCount: number;
  page: number;
  pageSize: number;
}

const STAGE_LABELS: Record<string, { label: string; color: string }> = {
  FETCH:    { label: '수집중',   color: 'bg-accent/10 text-accent border border-accent/30' },
  EMBED:    { label: '임베딩',   color: 'bg-warning/10 text-warning border border-warning/30' },
  INDEX:    { label: '인덱싱',   color: 'bg-warning/10 text-warning border border-warning/30' },
  COMPLETE: { label: '완료',     color: 'bg-success/10 text-success border border-success/30' },
  FAILED:   { label: '실패',     color: 'bg-error/10 text-error border border-error/30' },
};

const STEPS = ['업로드', '전처리', '인덱싱'];

const MockBadge = () => (
  <span className="text-[10px] font-mono text-warning bg-warning/10 border border-warning/20 rounded px-2 py-0.5">
    목업 데이터
  </span>
);

export default function UploadPage() {
  const [crawlUrl, setCrawlUrl] = useState('');
  const [crawlPeriod, setCrawlPeriod] = useState<'daily' | 'weekly'>('daily');
  const [excludePath, setExcludePath] = useState('');

  const { data, isLoading } = useSWR<PagedResponse<IngestionJob>>(
    '/api/admin/ingestion-jobs?page_size=10',
    fetcher
  );

  const jobs = data?.items ?? [];

  return (
    <div className="space-y-6">
      <PageGuide
        description="문서를 업로드하거나 웹 크롤링 소스를 등록하는 화면입니다."
        tips={[
          "업로드 후 인덱싱 상태가 '완료'로 바뀔 때까지 기다린 뒤 검색 테스트를 진행하세요.",
          "업로드 이력에서 '실패' 상태의 파일은 오류 원인을 확인 후 재처리하세요.",
          "HWP 파일은 파싱 오류가 잦으니 PDF로 변환 후 업로드를 권장합니다.",
        ]}
      />
      <div className="flex items-center justify-between">
        <h2 className="text-text-primary font-semibold text-lg">데이터 업로드</h2>
        <MockBadge />
      </div>

      {/* 파일 업로드 */}
      <Card>
        <CardHeader>
          <CardTitle>파일 업로드</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <div className="border-2 border-dashed border-bg-border rounded-xl p-10 text-center">
            <span className="material-symbols-outlined text-4xl text-text-muted">upload_file</span>
            <p className="text-text-secondary text-sm mt-2">HWP / PDF / XLSX / DOCX</p>
            <p className="text-text-muted text-xs mt-1">드래그앤드롭 또는 클릭하여 업로드</p>
            <button
              disabled
              className="mt-4 inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium opacity-50 cursor-not-allowed"
            >
              업로드 (API 연동 후 활성화)
            </button>
          </div>
        </div>
      </Card>

      {/* 웹 크롤링 설정 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">웹 크롤링 설정</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-4">
          <div>
            <label className="block text-xs text-text-secondary mb-1">크롤링 URL</label>
            <input
              type="url"
              value={crawlUrl}
              onChange={(e) => setCrawlUrl(e.target.value)}
              placeholder="https://example.go.kr"
              className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs text-text-secondary mb-1">수집 주기</label>
              <div className="flex gap-3">
                {(['daily', 'weekly'] as const).map((p) => (
                  <label key={p} className="flex items-center gap-1.5 cursor-pointer">
                    <input
                      type="radio"
                      name="period"
                      value={p}
                      checked={crawlPeriod === p}
                      onChange={() => setCrawlPeriod(p)}
                      className="accent-accent"
                    />
                    <span className="text-xs text-text-secondary">
                      {p === 'daily' ? '매일' : '매주'}
                    </span>
                  </label>
                ))}
              </div>
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">제외 경로</label>
              <input
                type="text"
                value={excludePath}
                onChange={(e) => setExcludePath(e.target.value)}
                placeholder="/board, /login"
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent"
              />
            </div>
          </div>
          <button
            disabled
            className="px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium opacity-50 cursor-not-allowed"
          >
            등록
          </button>
        </div>
      </Card>

      {/* 수동 재인덱싱 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">수동 재인덱싱</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-4">
          {/* 스텝 인디케이터 */}
          <div className="flex items-center gap-0">
            {STEPS.map((step, i) => (
              <div key={step} className="flex items-center">
                <div className="flex flex-col items-center">
                  <div className="w-7 h-7 rounded-full bg-bg-elevated border border-bg-border flex items-center justify-center">
                    <span className="font-mono text-[10px] text-text-muted">{i + 1}</span>
                  </div>
                  <span className="text-[10px] text-text-muted mt-1">{step}</span>
                </div>
                {i < STEPS.length - 1 && (
                  <div className="w-16 h-px bg-bg-border mx-2 mb-4" />
                )}
              </div>
            ))}
          </div>
          <button
            disabled
            className="px-4 py-2 rounded-lg bg-bg-elevated text-text-secondary text-sm font-medium opacity-50 cursor-not-allowed border border-bg-border"
          >
            즉시 실행
          </button>
        </div>
      </Card>

      {/* 업로드 이력 */}
      <Card>
        <CardHeader>
          <CardTitle>업로드 이력 (실제 API)</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          {isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Spinner />
            </div>
          ) : jobs.length === 0 ? (
            <p className="text-text-muted text-sm py-4">인제스션 잡이 없습니다.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-bg-border">
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">Job ID</th>
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">상태</th>
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">생성일</th>
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">재처리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-bg-border">
                {jobs.map((job) => {
                  const stage = STAGE_LABELS[job.jobStage] ?? { label: job.jobStage, color: 'bg-bg-elevated text-text-secondary' };
                  return (
                    <tr key={job.id}>
                      <td className="py-2.5 font-mono text-[11px] text-text-muted">{job.id}</td>
                      <td className="py-2.5">
                        <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded-full ${stage.color}`}>
                          {stage.label}
                        </span>
                      </td>
                      <td className="py-2.5 text-xs text-text-secondary">
                        {new Date(job.requestedAt).toLocaleDateString('ko-KR')}
                      </td>
                      <td className="py-2.5">
                        <button
                          disabled
                          className="text-[11px] text-accent opacity-50 cursor-not-allowed"
                        >
                          재처리
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </Card>
    </div>
  );
}
