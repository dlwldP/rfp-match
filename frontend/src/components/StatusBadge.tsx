import type { MatchStatusLabel } from '../api/types';
import { STATUS_COLOR } from '../format';

/** 판정 배지. 색 옆에 반드시 한글 라벨을 붙여 색만으로 의미를 전달하지 않는다. */
export function StatusBadge({ status }: { status: MatchStatusLabel }) {
  return (
    <span className="badge">
      <span className="badge__dot" style={{ background: STATUS_COLOR[status] }} aria-hidden="true" />
      {status}
    </span>
  );
}

export function TextBadge({ label, tone = 'muted' }: { label: string; tone?: 'muted' | 'accent' }) {
  return (
    <span className="badge badge--muted" style={tone === 'accent' ? { color: '#1f4b99' } : undefined}>
      {label}
    </span>
  );
}
