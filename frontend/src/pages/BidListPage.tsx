import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { bidApi, toErrorMessage } from '../api/client';
import type { BidSummary, PageResponse } from '../api/types';
import { TextBadge } from '../components/StatusBadge';
import { bidStatusLabel, daysLeft, formatDate, formatPrice } from '../format';

const PAGE_SIZE = 20;

export function BidListPage() {
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState('OPEN');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<BidSummary> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await bidApi.list({ keyword: keyword || undefined, status: status || undefined, page, size: PAGE_SIZE }));
    } catch (err) {
      setError(toErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [keyword, status, page]);

  useEffect(() => {
    void load();
  }, [load]);

  const sync = async () => {
    setNotice(null);
    setError(null);
    try {
      const report = await bidApi.sync();
      setNotice(
        `공고 동기화 완료 — 수집 ${report.fetched}건 (신규 ${report.created}, 갱신 ${report.updated})` +
          (report.failedKeywords.length > 0 ? ` / 실패 키워드: ${report.failedKeywords.join(', ')}` : ''),
      );
      setPage(0);
      await load();
    } catch (err) {
      setError(toErrorMessage(err));
    }
  };

  return (
    <div className="page">
      <h1 className="page__title">입찰공고</h1>
      <p className="page__subtitle">
        Open API로 수집한 보안장비 관련 공고입니다. 공고를 열어 RFP 요구사항을 등록하고 제품과 매칭하세요.
      </p>

      {error && <div className="alert alert--error">{error}</div>}
      {notice && <div className="alert alert--info">{notice}</div>}

      <form
        className="toolbar"
        onSubmit={(event) => {
          event.preventDefault();
          setPage(0);
          void load();
        }}
      >
        <input
          placeholder="공고명 검색 (예: 침입방지, 방화벽)"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          style={{ minWidth: 240 }}
        />
        <select value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }}>
          <option value="">전체</option>
          <option value="OPEN">진행중</option>
          <option value="CLOSED">마감</option>
        </select>
        <button type="submit" className="primary">검색</button>
        <button type="button" onClick={() => void sync()}>공고 동기화</button>
      </form>

      <div className="card">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>공고명</th>
                <th>공고기관</th>
                <th className="num">추정가격</th>
                <th>마감일</th>
                <th className="num">요구사항</th>
                <th>매칭</th>
              </tr>
            </thead>
            <tbody>
              {data?.content.map((bid) => {
                const remaining = daysLeft(bid.closeDate);
                return (
                  <tr key={bid.bidId}>
                    <td>
                      <Link to={`/bids/${bid.bidId}`}>{bid.title}</Link>
                      <div className="muted mono" style={{ fontSize: 12 }}>공고번호 {bid.bidNtceNo}</div>
                    </td>
                    <td>{bid.institution ?? '-'}</td>
                    <td className="num">{formatPrice(bid.estimatedPrice)}</td>
                    <td>
                      {formatDate(bid.closeDate)}
                      <div className="muted" style={{ fontSize: 12 }}>
                        {bid.status === 'OPEN' && remaining !== null
                          ? `D-${Math.max(remaining, 0)}`
                          : bidStatusLabel(bid.status)}
                      </div>
                    </td>
                    <td className="num">{bid.requirementCount}건</td>
                    <td>
                      <TextBadge label={bid.matchStatus} tone={bid.matchStatus === '매칭완료' ? 'accent' : 'muted'} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {loading && <div className="empty">불러오는 중…</div>}
        {!loading && data?.content.length === 0 && (
          <div className="empty">
            공고가 없습니다. 공고 동기화를 실행하거나 검색 조건을 바꿔 보세요.
          </div>
        )}

        {data && data.totalPages > 1 && (
          <div className="toolbar" style={{ marginTop: 16, marginBottom: 0, justifyContent: 'flex-end' }}>
            <button disabled={page === 0} onClick={() => setPage((prev) => prev - 1)}>이전</button>
            <span className="muted mono">
              {data.page + 1} / {data.totalPages} (총 {data.totalElements}건)
            </span>
            <button disabled={page >= data.totalPages - 1} onClick={() => setPage((prev) => prev + 1)}>다음</button>
          </div>
        )}
      </div>
    </div>
  );
}
