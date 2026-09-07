import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { bidApi, matchApi, productApi, requirementApi, toErrorMessage } from '../api/client';
import type { BidDetail, MatchResult, Product, RequirementInput } from '../api/types';
import { GapAnalysisChart } from '../components/GapAnalysisChart';
import { StatusBadge } from '../components/StatusBadge';
import { bidStatusLabel, formatDateTime, formatPrice } from '../format';

const EMPTY_ROW: RequirementInput = {
  category: '',
  description: '',
  requiredValue: '',
  unit: '',
  mandatory: true,
};

export function BidDetailPage() {
  const { bidId: bidIdParam } = useParams();
  const bidId = Number(bidIdParam);

  const [bid, setBid] = useState<BidDetail | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);
  const [match, setMatch] = useState<MatchResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const loadBid = useCallback(async () => {
    setError(null);
    try {
      setBid(await bidApi.detail(bidId));
    } catch (err) {
      setError(toErrorMessage(err));
    }
  }, [bidId]);

  useEffect(() => {
    void (async () => {
      setLoading(true);
      await loadBid();
      try {
        const list = await productApi.list();
        setProducts(list);
        setSelectedProductId((prev) => prev ?? list[0]?.productId ?? null);
      } catch (err) {
        setError(toErrorMessage(err));
      } finally {
        setLoading(false);
      }
    })();
  }, [loadBid]);

  // 제품을 바꾸면 저장돼 있던 갭분석 결과를 먼저 보여 주고, 없으면 비운다.
  useEffect(() => {
    if (!selectedProductId || Number.isNaN(bidId)) {
      setMatch(null);
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const result = await matchApi.result(bidId, selectedProductId);
        if (!cancelled) setMatch(result);
      } catch {
        if (!cancelled) setMatch(null); // 아직 매칭을 실행하지 않은 조합
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [bidId, selectedProductId]);

  const runMatch = async () => {
    if (!selectedProductId) return;
    setError(null);
    try {
      setMatch(await matchApi.run(bidId, selectedProductId));
      await loadBid();
    } catch (err) {
      setError(toErrorMessage(err));
    }
  };

  const addRequirements = async (rows: RequirementInput[]) => {
    setError(null);
    try {
      await requirementApi.register(bidId, rows);
      setMatch(null); // 요구사항이 바뀌면 기존 갭분석은 무효
      await loadBid();
      return true;
    } catch (err) {
      setError(toErrorMessage(err));
      return false;
    }
  };

  const removeRequirement = async (requirementId: number) => {
    setError(null);
    try {
      await requirementApi.remove(bidId, requirementId);
      setMatch(null);
      await loadBid();
    } catch (err) {
      setError(toErrorMessage(err));
    }
  };

  if (loading) {
    return <div className="page"><div className="empty">불러오는 중…</div></div>;
  }

  if (!bid) {
    return (
      <div className="page">
        {error && <div className="alert alert--error">{error}</div>}
        <Link to="/bids">← 공고 목록으로</Link>
      </div>
    );
  }

  return (
    <div className="page">
      <Link to="/bids">← 공고 목록</Link>
      <h1 className="page__title" style={{ marginTop: 10 }}>{bid.title}</h1>
      <p className="page__subtitle mono">
        공고번호 {bid.bidNtceNo} · {bid.institution ?? '기관 미상'} · 마감 {formatDateTime(bid.closeDate)} (
        {bidStatusLabel(bid.status)}) · 추정가격 {formatPrice(bid.estimatedPrice)}
      </p>

      {error && <div className="alert alert--error">{error}</div>}

      <section className="card">
        <h2 className="card__title">
          RFP 요구사항 <span className="card__hint">{bid.requirements.length}건 · 공고 첨부 RFP를 읽고 담당자가 등록</span>
        </h2>

        {bid.requirements.length === 0 ? (
          <div className="empty">등록된 요구사항이 없습니다. 아래에서 항목을 추가하세요.</div>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>분류</th>
                  <th>요구사항</th>
                  <th>요구값</th>
                  <th>필수</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {bid.requirements.map((item) => (
                  <tr key={item.requirementId}>
                    <td>{item.category}</td>
                    <td>{item.description}</td>
                    <td className="mono">
                      {item.requiredValue}
                      {item.unit ?? ''}
                    </td>
                    <td>{item.mandatory ? '필수' : '선택'}</td>
                    <td>
                      <button className="link danger" onClick={() => void removeRequirement(item.requirementId)}>
                        삭제
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <RequirementForm onSubmit={addRequirements} />
      </section>

      <section className="card">
        <h2 className="card__title">
          갭분석
          <span className="card__hint">요구사항과 제품 스펙을 대조해 충족 여부를 판정합니다</span>
        </h2>

        {products.length === 0 ? (
          <div className="alert alert--warn">
            등록된 제품이 없습니다. <Link to="/products">제품 스펙</Link>에서 자사 제품을 먼저 등록하세요.
          </div>
        ) : (
          <div className="toolbar">
            <label htmlFor="product-select">대상 제품</label>
            <select
              id="product-select"
              value={selectedProductId ?? ''}
              onChange={(event) => setSelectedProductId(Number(event.target.value))}
            >
              {products.map((product) => (
                <option key={product.productId} value={product.productId}>
                  {product.productName} ({product.category})
                </option>
              ))}
            </select>
            <button className="primary" onClick={() => void runMatch()} disabled={bid.requirements.length === 0}>
              매칭 실행
            </button>
            {bid.requirements.length === 0 && <span className="muted">요구사항을 먼저 등록하세요.</span>}
          </div>
        )}

        {match ? (
          <>
            <GapAnalysisChart summary={match.summary} />
            <div className="table-scroll" style={{ marginTop: 18 }}>
              <table>
                <thead>
                  <tr>
                    <th>판정</th>
                    <th>분류</th>
                    <th>요구사항</th>
                    <th>요구값</th>
                    <th>제품 스펙</th>
                    <th>근거</th>
                  </tr>
                </thead>
                <tbody>
                  {match.results.map((item) => (
                    <tr key={item.requirementId}>
                      <td>
                        <StatusBadge status={item.status} />
                        {item.mandatory && <div className="muted" style={{ fontSize: 11 }}>필수</div>}
                      </td>
                      <td>{item.category}</td>
                      <td>{item.description}</td>
                      <td className="mono">{item.requiredValue}</td>
                      <td className="mono">{item.productValue ?? '-'}</td>
                      <td className="muted">{item.note}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="muted" style={{ fontSize: 12, marginBottom: 0 }}>
              매칭 실행 시각 {formatDateTime(match.matchedAt)} · 판정이 "확인불가"인 항목은 RFP 원문과 대조해 담당자가 최종 판단해야 합니다.
            </p>
          </>
        ) : (
          products.length > 0 && <div className="empty">아직 매칭 결과가 없습니다. [매칭 실행]을 눌러 갭분석표를 만드세요.</div>
        )}
      </section>
    </div>
  );
}

/** 요구사항 여러 줄을 한 번에 입력해 등록한다. */
function RequirementForm({ onSubmit }: { onSubmit: (rows: RequirementInput[]) => Promise<boolean> }) {
  const [rows, setRows] = useState<RequirementInput[]>([{ ...EMPTY_ROW }]);
  const [submitting, setSubmitting] = useState(false);

  const update = (index: number, patch: Partial<RequirementInput>) => {
    setRows((prev) => prev.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  };

  const submit = async () => {
    const filled = rows
      .filter((row) => row.category.trim() && row.description.trim() && row.requiredValue.trim())
      .map((row) => ({ ...row, unit: row.unit?.trim() ? row.unit.trim() : null }));
    if (filled.length === 0) return;

    setSubmitting(true);
    const ok = await onSubmit(filled);
    setSubmitting(false);
    if (ok) setRows([{ ...EMPTY_ROW }]);
  };

  return (
    <div style={{ marginTop: 18 }}>
      <div className="row-grid row-grid--requirement row-grid__head">
        <div>분류</div>
        <div>요구사항 문구 (RFP 원문)</div>
        <div>요구값</div>
        <div>단위</div>
        <div>필수</div>
        <div />
      </div>

      {rows.map((row, index) => (
        <div className="row-grid row-grid--requirement" key={index}>
          <input
            placeholder="처리성능"
            value={row.category}
            onChange={(event) => update(index, { category: event.target.value })}
          />
          <input
            placeholder="네트워크 처리량 10Gbps 이상"
            value={row.description}
            onChange={(event) => update(index, { description: event.target.value })}
          />
          <input
            placeholder="10"
            value={row.requiredValue}
            onChange={(event) => update(index, { requiredValue: event.target.value })}
          />
          <input
            placeholder="Gbps"
            value={row.unit ?? ''}
            onChange={(event) => update(index, { unit: event.target.value })}
          />
          <label className="checkbox-cell">
            <input
              type="checkbox"
              checked={row.mandatory}
              onChange={(event) => update(index, { mandatory: event.target.checked })}
            />
            필수
          </label>
          <button
            className="link danger"
            onClick={() => setRows((prev) => (prev.length === 1 ? prev : prev.filter((_, i) => i !== index)))}
            disabled={rows.length === 1}
          >
            행 삭제
          </button>
        </div>
      ))}

      <div className="toolbar" style={{ marginTop: 12, marginBottom: 0 }}>
        <button onClick={() => setRows((prev) => [...prev, { ...EMPTY_ROW }])}>+ 행 추가</button>
        <button className="primary" onClick={() => void submit()} disabled={submitting}>
          {submitting ? '등록 중…' : '요구사항 등록'}
        </button>
        <span className="muted">등록하면 기존 갭분석 결과는 초기화됩니다.</span>
      </div>
    </div>
  );
}
