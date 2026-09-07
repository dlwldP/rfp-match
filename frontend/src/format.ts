import type { BidStatus, MatchStatusLabel } from './api/types';

/** 갭분석 판정별 색상. 상태 색상은 고정 배정이며 항상 라벨과 함께 쓴다(색만으로 의미를 전달하지 않는다). */
export const STATUS_COLOR: Record<MatchStatusLabel, string> = {
  충족: '#0ca30c',
  부분충족: '#fab219',
  미충족: '#d03b3b',
  확인불가: '#898781',
};

export const STATUS_ORDER: MatchStatusLabel[] = ['충족', '부분충족', '미충족', '확인불가'];

export function formatDateTime(value: string | null): string {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

export function formatDate(value: string | null): string {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(date);
}

/** 추정가격은 억/만 단위가 영업 담당자에게 훨씬 빨리 읽힌다. */
export function formatPrice(value: number | null): string {
  if (value == null) return '-';
  if (value >= 100_000_000) {
    const eok = value / 100_000_000;
    return `${eok.toFixed(eok >= 10 ? 0 : 1)}억원`;
  }
  if (value >= 10_000) {
    return `${Math.round(value / 10_000).toLocaleString('ko-KR')}만원`;
  }
  return `${value.toLocaleString('ko-KR')}원`;
}

export function bidStatusLabel(status: BidStatus): string {
  switch (status) {
    case 'OPEN':
      return '진행중';
    case 'CLOSED':
      return '마감';
    default:
      return '미정';
  }
}

/** 마감까지 남은 일수. 임박한 공고를 목록에서 바로 알아보게 한다. */
export function daysLeft(closeDate: string | null): number | null {
  if (!closeDate) return null;
  const close = new Date(closeDate).getTime();
  if (Number.isNaN(close)) return null;
  return Math.ceil((close - Date.now()) / (1000 * 60 * 60 * 24));
}
