import { Bar, BarChart, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { MatchStatusLabel, MatchSummary } from '../api/types';
import { STATUS_COLOR, STATUS_ORDER } from '../format';

interface Props {
  summary: MatchSummary;
}

interface Segment {
  status: MatchStatusLabel;
  count: number;
}

/**
 * 갭분석 구성비. 판정 4종의 비율 하나를 보여 주는 자리라 원형 차트 대신
 * 가로 100% 누적 막대 한 줄로 그린다(같은 폭에서 비교가 정확하고 라벨을 붙이기 쉽다).
 */
export function GapAnalysisChart({ summary }: Props) {
  const segments: Segment[] = [
    { status: '충족', count: summary.satisfied },
    { status: '부분충족', count: summary.partial },
    { status: '미충족', count: summary.unsatisfied },
    { status: '확인불가', count: summary.unknown },
  ];
  const total = summary.totalCount || 1;

  // 누적 막대 한 줄로 그리기 위해 판정별 값을 한 행에 펼친다.
  const row = segments.reduce<Record<string, number>>((acc, segment) => {
    acc[segment.status] = segment.count;
    return acc;
  }, { name: 0 });

  return (
    <div>
      <div className="stat-row">
        <div className="stat">
          <div className="stat__label">충족률</div>
          <div className="stat__value">
            {summary.satisfactionRate}
            <small>%</small>
          </div>
        </div>
        <div className="stat">
          <div className="stat__label">전체 요구사항</div>
          <div className="stat__value">
            {summary.totalCount}
            <small>건</small>
          </div>
        </div>
        <div className="stat">
          <div className="stat__label">필수 미충족</div>
          <div className="stat__value" style={{ color: summary.mandatoryUnsatisfied > 0 ? STATUS_COLOR.미충족 : undefined }}>
            {summary.mandatoryUnsatisfied}
            <small>건</small>
          </div>
        </div>
        <div className="stat">
          <div className="stat__label">입찰 참여 판단</div>
          <div className="stat__value" style={{ fontSize: 18, color: summary.biddable ? STATUS_COLOR.충족 : STATUS_COLOR.미충족 }}>
            {summary.biddable ? '참여 가능' : '필수항목 보완 필요'}
          </div>
        </div>
      </div>

      <ResponsiveContainer width="100%" height={56}>
        <BarChart data={[row]} layout="vertical" margin={{ top: 0, right: 0, bottom: 0, left: 0 }}>
          <XAxis type="number" domain={[0, total]} hide />
          <YAxis type="category" dataKey="name" hide />
          <Tooltip
            cursor={{ fill: 'transparent' }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null;
              return (
                <div className="tooltip">
                  {payload.map((entry) => (
                    <div key={String(entry.name)}>
                      <strong>{String(entry.name)}</strong> {Number(entry.value)}건 (
                      {Math.round((Number(entry.value) / total) * 100)}%)
                    </div>
                  ))}
                </div>
              );
            }}
          />
          {STATUS_ORDER.map((status) => (
            <Bar key={status} dataKey={status} stackId="gap" radius={4} barSize={28}>
              {/* 세그먼트 사이 2px 배경색 테두리로 경계를 만든다 */}
              <Cell fill={STATUS_COLOR[status]} stroke="#fcfcfb" strokeWidth={2} />
            </Bar>
          ))}
        </BarChart>
      </ResponsiveContainer>

      <div className="chart-legend">
        {segments.map((segment) => (
          <span className="chart-legend__item" key={segment.status}>
            <span className="chart-legend__swatch" style={{ background: STATUS_COLOR[segment.status] }} aria-hidden="true" />
            {segment.status} {segment.count}건
          </span>
        ))}
      </div>
    </div>
  );
}
