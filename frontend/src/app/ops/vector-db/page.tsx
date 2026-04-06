"use client";

import { useState } from "react";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { KpiCard } from "@/components/charts/KpiCard";
import { PageGuide } from "@/components/ui/PageGuide";

type EmbeddingModel = 'bge-m3' | 'text-embedding-3-small' | 'text-embedding-ada-002';

const MockBadge = () => (
  <span className="text-[10px] font-mono text-warning bg-warning/10 border border-warning/20 rounded px-2 py-0.5">
    목업 데이터
  </span>
);

export default function VectorDbPage() {
  const [model, setModel] = useState<EmbeddingModel>('bge-m3');
  const [chunkId, setChunkId] = useState('');

  const handleChunkSearch = () => {
    alert('청크 검색 기능은 준비 중입니다.');
  };

  return (
    <div className="space-y-6">
      <PageGuide
        description="임베딩 모델 설정과 벡터 인덱스 상태를 관리하는 화면입니다."
        tips={[
          "임베딩 모델 변경 시 전체 재인덱싱이 필요하므로 신중하게 결정하세요.",
          "Memory 사용률이 80% 이상이면 서버 증설 또는 청크 수 축소를 검토하세요.",
          "Embedding Drift 경보가 발생하면 최근 추가 문서의 품질을 점검하세요.",
        ]}
      />
      <div className="flex items-center justify-between">
        <h2 className="text-text-primary font-semibold text-lg">벡터 DB 관리</h2>
        <MockBadge />
      </div>

      {/* 임베딩 모델 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">임베딩 모델</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-3">
          <div>
            <label className="block text-xs text-text-secondary mb-1">모델 선택</label>
            <select
              value={model}
              onChange={(e) => setModel(e.target.value as EmbeddingModel)}
              className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
            >
              <option value="bge-m3">bge-m3 (Ollama) — 현재 사용 중</option>
              <option value="text-embedding-3-small">text-embedding-3-small (OpenAI)</option>
              <option value="text-embedding-ada-002">text-embedding-ada-002 (OpenAI)</option>
            </select>
          </div>
          {model !== 'bge-m3' && (
            <p className="text-[11px] text-warning bg-warning/10 border border-warning/20 rounded-lg px-3 py-2">
              ⚠ 모델 변경 시 전체 재인덱싱이 필요합니다
            </p>
          )}
          <div className="flex items-center gap-2">
            <button
              disabled
              className="px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium opacity-50 cursor-not-allowed"
            >
              변경
            </button>
            <span className="text-[11px] text-text-muted">현재: bge-m3 (Ollama)</span>
          </div>
        </div>
      </Card>

      {/* 인덱스 상태 KPI */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <KpiCard
          label="MEMORY USAGE"
          value="2.4 GB"
          sub="/ 8 GB"
          status="ok"
          progressValue={30}
          help="pgvector 인덱스 메모리 사용량"
        />
        <KpiCard
          label="QPS"
          value="124"
          sub="req/s"
          status="ok"
          progressValue={62}
          help="초당 벡터 검색 요청 수"
        />
        <KpiCard
          label="FALLBACK RATE"
          value="3.2%"
          status="ok"
          progressValue={6}
          help="벡터 검색 실패 후 폴백 비율. 5% 미만이면 정상."
        />
      </div>

      {/* Embedding Drift */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">Embedding Drift 모니터링</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4 space-y-3">
          <div className="flex items-center gap-3 p-4 bg-bg-elevated rounded-lg">
            <span className="material-symbols-outlined text-success text-xl">trending_flat</span>
            <div>
              <p className="text-sm font-medium text-text-primary">주간 분포 이동: +0.02 (정상 범위)</p>
              <p className="text-[11px] text-text-muted mt-0.5">기준 임계값: 0.05 이상 시 경보</p>
            </div>
          </div>
          <div>
            <label className="block text-xs text-text-secondary mb-1">경보 임계값</label>
            <input
              type="number"
              defaultValue={0.05}
              step={0.01}
              min={0.01}
              max={1.0}
              disabled
              className="w-32 bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-muted opacity-50 cursor-not-allowed"
            />
          </div>
        </div>
      </Card>

      {/* 청크 검색 */}
      <Card>
        <CardHeader>
          <CardTitle tag="목업">청크 검색</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          <div className="flex gap-2">
            <input
              type="text"
              value={chunkId}
              onChange={(e) => setChunkId(e.target.value)}
              placeholder="chunk_id 입력 (예: chunk_abc12345)"
              className="flex-1 bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent"
            />
            <button
              onClick={handleChunkSearch}
              className="px-4 py-2 rounded-lg bg-bg-elevated border border-bg-border text-text-secondary text-sm font-medium hover:border-accent hover:text-accent transition-colors"
            >
              검색
            </button>
          </div>
          <p className="text-[11px] text-text-muted mt-2">청크 검색 및 삭제 기능은 API 연동 예정입니다.</p>
        </div>
      </Card>
    </div>
  );
}
