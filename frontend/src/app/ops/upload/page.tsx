"use client";

import { useRef, useState, useCallback, useEffect } from "react";
import useSWR from "swr";
import { fetcher, ingestionApi, documentApi } from "@/lib/api";
import { Card, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { PageGuide } from "@/components/ui/PageGuide";
import type { IngestionJob, CrawlSource } from "@/lib/types";
import { useFilter } from "@/lib/filter-context";

interface CrawlSourceListResponse {
  items: CrawlSource[];
  total: number;
}

interface IngestionJobListResponse {
  items: IngestionJob[];
  total: number;
}

const STAGE_LABELS: Record<string, { label: string; color: string }> = {
  fetch:     { label: "수집중",  color: "bg-accent/10 text-accent border border-accent/30" },
  extract:   { label: "추출중",  color: "bg-accent/10 text-accent border border-accent/30" },
  normalize: { label: "정규화",  color: "bg-warning/10 text-warning border border-warning/30" },
  chunk:     { label: "청킹",    color: "bg-warning/10 text-warning border border-warning/30" },
  embed:     { label: "임베딩",  color: "bg-warning/10 text-warning border border-warning/30" },
  index:     { label: "인덱싱",  color: "bg-warning/10 text-warning border border-warning/30" },
  complete:  { label: "완료",    color: "bg-success/10 text-success border border-success/30" },
};

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  queued:         { label: "대기",   color: "bg-bg-elevated text-text-secondary border border-bg-border" },
  running:        { label: "실행중", color: "bg-accent/10 text-accent border border-accent/30" },
  succeeded:      { label: "성공",   color: "bg-success/10 text-success border border-success/30" },
  partial_success:{ label: "부분성공", color: "bg-warning/10 text-warning border border-warning/30" },
  failed:         { label: "실패",   color: "bg-error/10 text-error border border-error/30" },
  cancelled:      { label: "취소",   color: "bg-bg-elevated text-text-muted border border-bg-border" },
};

const REINDEX_STEPS = ["수집", "추출", "청킹", "임베딩", "인덱싱"];
const STAGE_STEP_MAP: Record<string, number> = {
  fetch: 0, extract: 1, normalize: 1, chunk: 2, embed: 3, index: 4, complete: 5,
};

const ALLOWED_EXTS = ".pdf,.hwp,.xlsx,.docx";

export default function UploadPage() {
  // ── 공유 상태: 기관 / 서비스 ──────────────────────────────────────────────────

  const { orgId: selectedOrgId, serviceId: selectedServiceId } = useFilter();

  // ── 크롤 소스 목록 (org 단위 fetch, client-side 필터링) ──────────────────────

  const { data: sourcesData, mutate: mutateSources } = useSWR<CrawlSourceListResponse>(
    selectedOrgId ? `/api/admin/crawl-sources?organization_id=${selectedOrgId}` : null,
    fetcher
  );
  const crawlSources = sourcesData?.items ?? [];

  // 선택된 서비스에 속하는 collectionName 유니크 목록
  const existingCollections = [
    ...new Set(
      crawlSources
        .filter((s) => s.serviceId === selectedServiceId && s.collectionName)
        .map((s) => s.collectionName!)
    ),
  ];

  useEffect(() => {
    setSelectedReindexCollection("");
    setUploadSelectedCollection("");
    setUploadNewCollection("");
    setUploadCollectionMode("existing");
    setCrawlSelectedCollection("");
    setCrawlNewCollection("");
    setCrawlCollectionMode("existing");
  }, [selectedOrgId, selectedServiceId]);

  // ── 파일 업로드 ───────────────────────────────────────────────────────────────

  const fileInputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [uploadCollectionMode, setUploadCollectionMode] = useState<"existing" | "new">("existing");
  const [uploadSelectedCollection, setUploadSelectedCollection] = useState("");
  const [uploadNewCollection, setUploadNewCollection] = useState("");
  const uploadCollectionName = uploadCollectionMode === "new" ? uploadNewCollection : uploadSelectedCollection;

  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<string | null>(null);
  const [uploadResult, setUploadResult] = useState<string | null>(null);

  const handleFileDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const dropped = Array.from(e.dataTransfer.files);
    if (dropped.length > 0) setSelectedFiles(dropped);
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const chosen = Array.from(e.target.files ?? []);
    if (chosen.length > 0) setSelectedFiles(chosen);
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0 || !selectedOrgId || !selectedServiceId) return;
    setUploading(true);
    setUploadResult(null);
    setUploadProgress(`업로드 중... (0/${selectedFiles.length})`);
    try {
      const results = await documentApi.upload(
        selectedFiles,
        selectedOrgId,
        selectedServiceId,
        uploadCollectionName || undefined,
      );
      setUploadProgress(null);
      setUploadResult(`${results.length}개 파일 업로드 완료`);
      setSelectedFiles([]);
      mutateJobs();
    } catch {
      setUploadProgress(null);
      setUploadResult("업로드 실패. 다시 시도해 주세요.");
    } finally {
      setUploading(false);
    }
  };

  // ── 웹 크롤링 등록 ────────────────────────────────────────────────────────────

  const ADAPTER_PATTERNS = [
    { pattern: /ggc\.go\.kr/, label: "경기도의회 전용 파서", color: "text-blue-400" },
  ] as const;

  function detectAdapter(url: string) {
    for (const { pattern, label, color } of ADAPTER_PATTERNS) {
      if (pattern.test(url)) return { label, color };
    }
    return { label: "범용 파서", color: "text-text-muted" };
  }

  const [crawlName, setCrawlName] = useState("");
  const [crawlCollectionMode, setCrawlCollectionMode] = useState<"existing" | "new">("existing");
  const [crawlSelectedCollection, setCrawlSelectedCollection] = useState("");
  const [crawlNewCollection, setCrawlNewCollection] = useState("");
  const crawlCollectionName = crawlCollectionMode === "new" ? crawlNewCollection : crawlSelectedCollection;

  const [crawlUrl, setCrawlUrl] = useState("");
  const [crawlSourceType, setCrawlSourceType] = useState("website");
  const [crawlRenderMode, setCrawlRenderMode] = useState("http_static");
  const [crawlCollectionModeField, setCrawlCollectionModeField] = useState("full");
  const [crawlPeriod, setCrawlPeriod] = useState<"daily" | "weekly">("daily");
  const [crawlSubmitting, setCrawlSubmitting] = useState(false);
  const [crawlResult, setCrawlResult] = useState<string | null>(null);

  const handleCrawlRegister = async () => {
    if (!crawlName || !crawlUrl || !selectedOrgId || !selectedServiceId) return;
    setCrawlSubmitting(true);
    setCrawlResult(null);
    try {
      await ingestionApi.createSource({
        organizationId: selectedOrgId,
        serviceId: selectedServiceId,
        name: crawlName,
        sourceType: crawlSourceType,
        sourceUri: crawlUrl,
        renderMode: crawlRenderMode,
        collectionMode: crawlCollectionModeField,
        scheduleExpr: crawlPeriod === "daily" ? "0 2 * * *" : "0 2 * * 0",
        collectionName: crawlCollectionName || undefined,
      });
      setCrawlResult("크롤 소스가 등록되었습니다.");
      setCrawlName("");
      setCrawlSelectedCollection("");
      setCrawlNewCollection("");
      setCrawlCollectionMode("existing");
      setCrawlUrl("");
      mutateSources();
    } catch {
      setCrawlResult("등록 실패. 다시 시도해 주세요.");
    } finally {
      setCrawlSubmitting(false);
    }
  };

  // ── 수동 재인덱싱 ─────────────────────────────────────────────────────────────

  const [selectedReindexCollection, setSelectedReindexCollection] = useState("");
  const [runningJobId, setRunningJobId] = useState<string | null>(null);
  const [reindexResult, setReindexResult] = useState<string | null>(null);
  const [reindexing, setReindexing] = useState(false);

  // 컬렉션 청크 삭제
  const [deletingChunks, setDeletingChunks] = useState(false);
  const [deleteChunksResult, setDeleteChunksResult] = useState<string | null>(null);

  const handleDeleteCollectionChunks = async () => {
    if (!selectedReindexCollection || !selectedServiceId) return;
    setDeletingChunks(true);
    setDeleteChunksResult(null);
    try {
      const params = new URLSearchParams({
        serviceId: selectedServiceId,
        collectionName: selectedReindexCollection,
      });
      const res = await fetch(`/api/admin/collections/chunks?${params}`, {
        method: "DELETE",
        headers: {
          "X-Admin-Session-Id": localStorage.getItem("sessionId") ?? "",
        },
      });
      if (!res.ok) throw new Error();
      const data: { deletedChunks: number; resetDocuments: number } = await res.json();
      setDeleteChunksResult(
        `청크 ${data.deletedChunks}개 삭제, 문서 ${data.resetDocuments}개 초기화 완료`
      );
      mutateJobs();
    } catch {
      setDeleteChunksResult("삭제 실패. 다시 시도해 주세요.");
    } finally {
      setDeletingChunks(false);
    }
  };

  // 리인덱싱용 컬렉션 목록 (existingCollections와 동일 소스)
  const reindexCollections = existingCollections;

  // 선택한 컬렉션에 속하는 소스 목록
  const sourcesInCollection = crawlSources.filter(
    (s) => s.serviceId === selectedServiceId && s.collectionName === selectedReindexCollection
  );

  const { data: runningJobData } = useSWR<IngestionJob>(
    runningJobId ? `/api/admin/ingestion-jobs/${runningJobId}` : null,
    fetcher,
    { refreshInterval: 3000 }
  );

  const currentStepIndex = runningJobData
    ? (STAGE_STEP_MAP[runningJobData.jobStage] ?? -1)
    : -1;

  const handleRunReindex = async () => {
    if (!selectedReindexCollection || sourcesInCollection.length === 0) return;
    setReindexing(true);
    setReindexResult(null);
    try {
      let lastJobId: string | null = null;
      for (const src of sourcesInCollection) {
        const result = await ingestionApi.runSource(src.id);
        lastJobId = result.jobId;
      }
      if (lastJobId) setRunningJobId(lastJobId);
      setReindexResult(`${sourcesInCollection.length}개 소스 실행 시작`);
      mutateJobs();
    } catch {
      setReindexResult("실행 실패. 다시 시도해 주세요.");
    } finally {
      setReindexing(false);
    }
  };

  // ── 업로드 이력 ───────────────────────────────────────────────────────────────

  const jobsQuery = (() => {
    const params = new URLSearchParams();
    if (selectedOrgId) params.set("organization_id", selectedOrgId);
    if (selectedServiceId) params.set("service_id", selectedServiceId);
    return params.toString();
  })();

  const { data: jobsData, mutate: mutateJobs } = useSWR<IngestionJobListResponse>(
    `/api/admin/ingestion-jobs${jobsQuery ? `?${jobsQuery}` : ""}`,
    fetcher,
    { refreshInterval: 5000 }
  );
  const jobs = jobsData?.items ?? [];

  const handleRerun = async (crawlSourceId: string) => {
    try {
      const result = await ingestionApi.runSource(crawlSourceId);
      setRunningJobId(result.jobId);
      mutateJobs();
    } catch {
      // 무시
    }
  };

  // ── 탭 ───────────────────────────────────────────────────────────────────────

  const [uploadTab, setUploadTab] = useState<"file" | "crawl">("file");

  // ── 렌더 ──────────────────────────────────────────────────────────────────────

  const isGateOpen = Boolean(selectedOrgId && selectedServiceId);

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
      <h2 className="text-text-primary font-semibold text-lg">데이터 업로드</h2>

      {!isGateOpen && (
        <div className="text-xs text-text-muted">
          상단 breadcrumb 필터에서 기관과 서비스를 먼저 선택하세요.
        </div>
      )}

      {/* 게이트: org + service 미선택 시 하위 섹션 비활성화 */}
      <div className={!isGateOpen ? "opacity-40 pointer-events-none select-none" : ""}>

        {/* 파일 업로드 / 웹 크롤링 탭 */}
        <Card>
          {/* 탭 헤더 */}
          <div className="flex border-b border-bg-border px-4">
            {([
              { key: "file", label: "파일 업로드", icon: "upload_file" },
              { key: "crawl", label: "웹 크롤링 등록", icon: "travel_explore" },
            ] as const).map((tab) => (
              <button
                key={tab.key}
                onClick={() => setUploadTab(tab.key)}
                className={`flex items-center gap-1.5 px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                  uploadTab === tab.key
                    ? "border-accent text-accent"
                    : "border-transparent text-text-muted hover:text-text-secondary"
                }`}
              >
                <span className="material-symbols-outlined text-base">{tab.icon}</span>
                {tab.label}
              </button>
            ))}
          </div>

          {/* 파일 업로드 탭 */}
          {uploadTab === "file" && (
          <div className="px-4 pb-4 pt-4 space-y-3">
            {/* 컬렉션 피커 — 파일 선택 위에 배치 */}
            <div className="space-y-2">
              <label className="block text-xs text-text-secondary mb-1">컬렉션</label>
              <select
                value={uploadCollectionMode === "new" ? "__new__" : uploadSelectedCollection}
                onChange={(e) => {
                  if (e.target.value === "__new__") {
                    setUploadCollectionMode("new");
                    setUploadSelectedCollection("");
                  } else {
                    setUploadCollectionMode("existing");
                    setUploadSelectedCollection(e.target.value);
                  }
                }}
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
              >
                <option value="">컬렉션을 선택하세요</option>
                {existingCollections.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
                <option value="__new__">＋ 새 컬렉션 추가</option>
              </select>
              {uploadCollectionMode === "new" && (
                <input
                  type="text"
                  value={uploadNewCollection}
                  onChange={(e) => setUploadNewCollection(e.target.value)}
                  placeholder="새 컬렉션 이름 입력"
                  className="w-full bg-bg-surface border border-accent/50 rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent"
                />
              )}
            </div>

            {/* 파일 드롭존 */}
            <div
              onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={handleFileDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`border-2 border-dashed rounded-xl p-10 text-center cursor-pointer transition-colors ${
                dragOver ? "border-accent bg-accent/5" : "border-bg-border hover:border-accent/50"
              }`}
            >
              <span className="material-symbols-outlined text-4xl text-text-muted">upload_file</span>
              {selectedFiles.length > 0 ? (
                <p className="text-text-primary text-sm mt-2 font-medium">{selectedFiles.length}개 파일 선택됨</p>
              ) : (
                <>
                  <p className="text-text-secondary text-sm mt-2">HWP / PDF / XLSX / DOCX</p>
                  <p className="text-text-muted text-xs mt-1">드래그앤드롭 또는 클릭하여 파일 선택 (다중 선택 가능)</p>
                </>
              )}
            </div>
            {selectedFiles.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {selectedFiles.map((f, i) => (
                  <span key={i} className="inline-flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full bg-bg-elevated border border-bg-border text-text-secondary">
                    <span className="material-symbols-outlined text-[12px]">description</span>
                    {f.name}
                  </span>
                ))}
              </div>
            )}
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept={ALLOWED_EXTS}
              onChange={handleFileChange}
              className="hidden"
            />
            {uploadProgress && (
              <p className="text-xs text-accent">{uploadProgress}</p>
            )}
            {uploadResult && (
              <p className={`text-xs ${uploadResult.includes("실패") ? "text-error" : "text-success"}`}>
                {uploadResult}
              </p>
            )}
            <button
              onClick={handleUpload}
              disabled={selectedFiles.length === 0 || !selectedOrgId || !selectedServiceId || uploading}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:bg-accent/90 transition-colors"
            >
              {uploading ? <Spinner /> : <span className="material-symbols-outlined text-base">upload</span>}
              {uploading ? (uploadProgress ?? "업로드 중...") : "업로드"}
            </button>
          </div>
          )}

          {/* 웹 크롤링 탭 */}
          {uploadTab === "crawl" && (
          <div className="px-4 pb-4 pt-4 space-y-4">
            {/* 컬렉션 피커 — 소스 이름/URL 입력 위에 배치 */}
            <div className="space-y-2">
              <label className="block text-xs text-text-secondary mb-1">컬렉션</label>
              <select
                value={crawlCollectionMode === "new" ? "__new__" : crawlSelectedCollection}
                onChange={(e) => {
                  if (e.target.value === "__new__") {
                    setCrawlCollectionMode("new");
                    setCrawlSelectedCollection("");
                  } else {
                    setCrawlCollectionMode("existing");
                    setCrawlSelectedCollection(e.target.value);
                  }
                }}
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
              >
                <option value="">컬렉션을 선택하세요</option>
                {existingCollections.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
                <option value="__new__">＋ 새 컬렉션 추가</option>
              </select>
              {crawlCollectionMode === "new" && (
                <input
                  type="text"
                  value={crawlNewCollection}
                  onChange={(e) => setCrawlNewCollection(e.target.value)}
                  placeholder="새 컬렉션 이름 입력"
                  className="w-full bg-bg-surface border border-accent/50 rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent"
                />
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs text-text-secondary mb-1">소스 이름</label>
                <input
                  type="text"
                  value={crawlName}
                  onChange={(e) => setCrawlName(e.target.value)}
                  placeholder="예: 민원서비스 공지사항"
                  className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent"
                />
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">소스 타입</label>
                <select
                  value={crawlSourceType}
                  onChange={(e) => setCrawlSourceType(e.target.value)}
                  className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                >
                  <option value="website">Website</option>
                  <option value="sitemap">Sitemap</option>
                  <option value="file_drop">File Drop</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block text-xs text-text-secondary mb-1">크롤링 URL</label>
              <input
                type="url"
                value={crawlUrl}
                onChange={(e) => setCrawlUrl(e.target.value)}
                placeholder="https://example.go.kr"
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:border-accent"
              />
              {crawlUrl && (() => {
                const adapter = detectAdapter(crawlUrl);
                return (
                  <p className={`text-xs mt-1 flex items-center gap-1 ${adapter.color}`}>
                    <span className="material-symbols-outlined text-sm">search</span>
                    {adapter.label}가 적용됩니다
                  </p>
                );
              })()}
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-xs text-text-secondary mb-1">렌더 모드</label>
                <select
                  value={crawlRenderMode}
                  onChange={(e) => setCrawlRenderMode(e.target.value)}
                  className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                >
                  <option value="http_static">HTTP Static</option>
                  <option value="browser_playwright">Playwright</option>
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">수집 모드</label>
                <select
                  value={crawlCollectionModeField}
                  onChange={(e) => setCrawlCollectionModeField(e.target.value)}
                  className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
                >
                  <option value="full">전체</option>
                  <option value="incremental">증분</option>
                </select>
              </div>
              <div>
                <label className="block text-xs text-text-secondary mb-1">수집 주기</label>
                <div className="flex gap-3 pt-2">
                  {(["daily", "weekly"] as const).map((p) => (
                    <label key={p} className="flex items-center gap-1.5 cursor-pointer">
                      <input
                        type="radio"
                        name="crawlPeriod"
                        value={p}
                        checked={crawlPeriod === p}
                        onChange={() => setCrawlPeriod(p)}
                        className="accent-accent"
                      />
                      <span className="text-xs text-text-secondary">{p === "daily" ? "매일" : "매주"}</span>
                    </label>
                  ))}
                </div>
              </div>
            </div>
            {crawlResult && (
              <p className={`text-xs ${crawlResult.includes("실패") ? "text-error" : "text-success"}`}>
                {crawlResult}
              </p>
            )}
            <button
              onClick={handleCrawlRegister}
              disabled={!crawlName || !crawlUrl || !selectedOrgId || !selectedServiceId || crawlSubmitting}
              className="px-4 py-2 rounded-lg bg-accent text-white text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:bg-accent/90 transition-colors"
            >
              {crawlSubmitting ? "등록 중..." : "등록"}
            </button>
          </div>
          )}
        </Card>

        {/* 수동 재인덱싱 */}
        <Card className="mt-6">
          <CardHeader>
            <CardTitle>수동 재인덱싱</CardTitle>
          </CardHeader>
          <div className="px-4 pb-4 space-y-4">
            <div>
              <label className="block text-xs text-text-secondary mb-1">컬렉션 선택</label>
              <select
                value={selectedReindexCollection}
                onChange={(e) => {
                  setSelectedReindexCollection(e.target.value);
                  setRunningJobId(null);
                  setReindexResult(null);
                }}
                className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-accent"
              >
                <option value="">컬렉션을 선택하세요</option>
                {reindexCollections.map((col) => (
                  <option key={col} value={col}>{col}</option>
                ))}
              </select>
              {selectedReindexCollection && sourcesInCollection.length > 0 && (
                <p className="text-xs text-text-muted mt-1">{sourcesInCollection.length}개 소스 포함</p>
              )}
            </div>

            {/* 스텝 인디케이터 */}
            <div className="flex items-center gap-0">
              {REINDEX_STEPS.map((step, i) => (
                <div key={step} className="flex items-center">
                  <div className="flex flex-col items-center">
                    <div className={`w-7 h-7 rounded-full flex items-center justify-center border ${
                      i < currentStepIndex
                        ? "bg-success/20 border-success/40"
                        : i === currentStepIndex
                        ? "bg-accent/20 border-accent/40"
                        : "bg-bg-elevated border-bg-border"
                    }`}>
                      {i < currentStepIndex ? (
                        <span className="material-symbols-outlined text-[14px] text-success">check</span>
                      ) : (
                        <span className="font-mono text-[10px] text-text-muted">{i + 1}</span>
                      )}
                    </div>
                    <span className="text-[10px] text-text-muted mt-1">{step}</span>
                  </div>
                  {i < REINDEX_STEPS.length - 1 && (
                    <div className={`w-16 h-px mx-2 mb-4 ${i < currentStepIndex ? "bg-success/40" : "bg-bg-border"}`} />
                  )}
                </div>
              ))}
            </div>

            {reindexResult && (
              <p className={`text-xs ${reindexResult.includes("실패") ? "text-error" : "text-success"}`}>
                {reindexResult}
              </p>
            )}
            {deleteChunksResult && (
              <p className={`text-xs ${deleteChunksResult.includes("실패") ? "text-error" : "text-success"}`}>
                {deleteChunksResult}
              </p>
            )}
            <div className="flex items-center gap-3">
              <button
                onClick={handleRunReindex}
                disabled={!selectedReindexCollection || sourcesInCollection.length === 0 || reindexing}
                className="px-4 py-2 rounded-lg bg-bg-elevated text-text-secondary text-sm font-medium border border-bg-border disabled:opacity-50 disabled:cursor-not-allowed hover:border-accent/50 hover:text-text-primary transition-colors"
              >
                {reindexing ? "실행 중..." : "즉시 실행"}
              </button>
              {selectedReindexCollection && (
                <button
                  onClick={handleDeleteCollectionChunks}
                  disabled={deletingChunks}
                  className="px-4 py-2 rounded-lg bg-error/10 text-error text-sm font-medium border border-error/30 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-error/20 transition-colors"
                >
                  {deletingChunks ? "삭제 중..." : "컬렉션 청크 삭제"}
                </button>
              )}
            </div>
          </div>
        </Card>

      </div>

      {/* 업로드 이력 */}
      <Card>
        <CardHeader>
          <CardTitle>업로드 이력</CardTitle>
        </CardHeader>
        <div className="px-4 pb-4">
          {jobs.length === 0 ? (
            <p className="text-text-muted text-sm py-4">인제스션 잡이 없습니다.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-bg-border">
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">컬렉션 / 소스</th>
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">단계</th>
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">상태</th>
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">트리거</th>
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">생성일</th>
                  <th className="text-left pb-2 text-[10px] font-mono uppercase tracking-wider text-text-muted">재처리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-bg-border">
                {jobs.map((job) => {
                  const stage = STAGE_LABELS[job.jobStage] ?? { label: job.jobStage, color: "bg-bg-elevated text-text-secondary" };
                  const status = STATUS_LABELS[job.status] ?? { label: job.status, color: "bg-bg-elevated text-text-secondary" };
                  const src = crawlSources.find((s) => s.id === job.crawlSourceId);
                  const triggerLabel: Record<string, string> = {
                    file_upload: "파일업로드",
                    manual: "수동",
                    scheduled: "스케줄",
                  };
                  return (
                    <tr key={job.id}>
                      <td className="py-2.5">
                        <div className="flex flex-col gap-0.5">
                          {src?.collectionName && (
                            <span className="inline-flex items-center gap-1 text-[10px] px-1.5 py-0.5 rounded bg-accent/10 text-accent border border-accent/20 w-fit">
                              {src.collectionName}
                            </span>
                          )}
                          <span className="text-[11px] text-text-muted font-mono truncate max-w-[180px]" title={src?.name ?? job.crawlSourceId}>
                            {src?.name ?? job.crawlSourceId}
                          </span>
                        </div>
                      </td>
                      <td className="py-2.5">
                        <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded-full ${stage.color}`}>
                          {stage.label}
                        </span>
                      </td>
                      <td className="py-2.5">
                        <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded-full ${status.color}`}>
                          {status.label}
                        </span>
                      </td>
                      <td className="py-2.5 text-[11px] text-text-muted">
                        {triggerLabel[job.triggerType] ?? job.triggerType}
                      </td>
                      <td className="py-2.5 text-xs text-text-secondary">
                        {new Date(job.requestedAt).toLocaleDateString("ko-KR")}
                      </td>
                      <td className="py-2.5">
                        <button
                          onClick={() => handleRerun(job.crawlSourceId)}
                          className="text-[11px] text-accent hover:underline"
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
