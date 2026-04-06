"use client";

import { useState } from "react";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { PageGuide } from "@/components/ui/PageGuide";

type ChunkAlgo = 'fixed' | 'sentence' | 'semantic';
type MaskMode = 'full' | 'partial';

const MOCK_ERRORS = [
  { file: 'welfare_guide_2026.hwp', error: '파싱 실패 — HWP 인코딩 오류', date: '2026-03-28' },
  { file: 'benefit_table.xlsx',     error: '빈 시트 감지',                date: '2026-03-27' },
];

const PII_OPTIONS = [
  { key: 'name',  label: '이름' },
  { key: 'phone', label: '전화번호' },
  { key: 'ssn',   label: '주민번호' },
  { key: 'email', label: '이메일' },
  { key: 'addr',  label: '주소' },
];

const MockBadge = () => (
  <span className="text-[10px] font-mono text-warning bg-warning/10 border border-warning/20 rounded px-2 py-0.5">
    목업 데이터
  </span>
);

export default function PreprocessingPage() {
  const [algo, setAlgo] = useState<ChunkAlgo>('fixed');
  const [tokenSize, setTokenSize] = useState(512);
  const [overlap, setOverlap] = useState(50);
  const [piiKeys, setPiiKeys] = useState<string[]>(['name', 'phone', 'ssn']);
  const [maskMode, setMaskMode] = useState<MaskMode>('full');

  const togglePii = (key: string) => {
    setPiiKeys((prev) =>
      prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key]
    );
  };

  return (
    <div className="space-y-6">
      <PageGuide
        description="문서 청킹 방식과 PII 비식별화 규칙을 설정하는 화면입니다."
        tips={[
          "청킹 설정 변경 시 전체 재인덱싱이 필요합니다 — 트래픽이 적은 시간에 진행하세요.",
          "토큰 크기를 줄이면 검색 정밀도가 높아지지만 문맥이 끊길 수 있습니다.",
          "개인정보(주민번호, 전화번호)가 포함된 문서라면 반드시 PII 비식별화를 활성화하세요.",
        ]}
      />
      <div className="flex items-center justify-between">
        <h2 className="text-text-primary font-semibold text-lg">데이터 전처리</h2>
        <MockBadge />
      </div>

      {/* 청킹 알고리즘 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">청킹 알고리즘</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-4">
          <div className="flex gap-6">
            {([
              { value: 'fixed',    label: '고정 크기' },
              { value: 'sentence', label: '문장 단위' },
              { value: 'semantic', label: '시맨틱' },
            ] as { value: ChunkAlgo; label: string }[]).map((opt) => (
              <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="algo"
                  value={opt.value}
                  checked={algo === opt.value}
                  onChange={() => setAlgo(opt.value)}
                  className="accent-accent"
                />
                <span className="text-sm text-text-secondary">{opt.label}</span>
              </label>
            ))}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs text-text-secondary mb-1">토큰 크기</label>
              <input
                type="number"
                value={tokenSize}
                onChange={(e) => setTokenSize(Number(e.target.value))}
                min={128}
                max={2048}
                step={64}
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
              />
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">오버랩 (토큰)</label>
              <input
                type="number"
                value={overlap}
                onChange={(e) => setOverlap(Number(e.target.value))}
                min={0}
                max={512}
                step={10}
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
              />
            </div>
          </div>

          <p className="text-[11px] text-warning bg-warning/10 border border-warning/20 rounded-lg px-3 py-2">
            ⚠ 변경 시 전체 재인덱싱이 필요합니다 (예상 소요: 약 30분)
          </p>

          <button
            disabled
            className="px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium opacity-50 cursor-not-allowed"
          >
            저장
          </button>
        </div>
      </Card>

      {/* PII 비식별화 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">PII 비식별화 규칙</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-4">
          <div>
            <p className="text-xs text-text-secondary mb-2">비식별화 대상</p>
            <div className="flex flex-wrap gap-4">
              {PII_OPTIONS.map((opt) => (
                <label key={opt.key} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={piiKeys.includes(opt.key)}
                    onChange={() => togglePii(opt.key)}
                    className="accent-accent"
                  />
                  <span className="text-sm text-text-secondary">{opt.label}</span>
                </label>
              ))}
            </div>
          </div>

          <div>
            <p className="text-xs text-text-secondary mb-2">마스킹 방식</p>
            <div className="flex gap-6">
              {([
                { value: 'full',    label: '전체 마스킹' },
                { value: 'partial', label: '부분 마스킹' },
              ] as { value: MaskMode; label: string }[]).map((opt) => (
                <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="mask"
                    value={opt.value}
                    checked={maskMode === opt.value}
                    onChange={() => setMaskMode(opt.value)}
                    className="accent-accent"
                  />
                  <span className="text-sm text-text-secondary">{opt.label}</span>
                </label>
              ))}
            </div>
          </div>

          <button
            disabled
            className="px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium opacity-50 cursor-not-allowed"
          >
            저장
          </button>
        </div>
      </Card>

      {/* Ingestion Error 로그 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">Ingestion 오류 로그</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-bg-border">
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">파일명</th>
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">오류 내용</th>
                <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">날짜</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-bg-border">
              {MOCK_ERRORS.map((err) => (
                <tr key={err.file}>
                  <td className="py-2.5 font-mono text-[11px] text-text-secondary">{err.file}</td>
                  <td className="py-2.5 text-xs text-error">{err.error}</td>
                  <td className="py-2.5 text-xs text-text-muted">{err.date}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {/* 문서 중복 감지 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">문서 중복 감지</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <div className="flex items-center gap-3 p-4 bg-bg-elevated rounded-lg">
            <span className="material-symbols-outlined text-success text-xl">check_circle</span>
            <div>
              <p className="text-sm font-medium text-text-primary">중복 의심 0건</p>
              <p className="text-[11px] text-text-muted mt-0.5">마지막 검사: 2026-03-28 09:00</p>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
