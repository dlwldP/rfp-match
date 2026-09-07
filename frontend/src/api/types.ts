/** 백엔드 API 응답 타입. Spring Boot DTO와 1:1로 대응한다. */

export type BidStatus = 'OPEN' | 'CLOSED' | 'UNKNOWN';

/** 매칭 판정 값. 백엔드가 한글 라벨로 내려준다. */
export type MatchStatusLabel = '충족' | '부분충족' | '미충족' | '확인불가';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface BidSummary {
  bidId: number;
  bidNtceNo: string;
  title: string;
  institution: string | null;
  closeDate: string | null;
  estimatedPrice: number | null;
  status: BidStatus;
  requirementCount: number;
  matchStatus: '매칭완료' | '미매칭';
}

export interface RequirementItem {
  requirementId: number;
  category: string;
  description: string;
  requiredValue: string;
  unit: string | null;
  mandatory: boolean;
}

export interface BidDetail {
  bidId: number;
  bidNtceNo: string;
  title: string;
  institution: string | null;
  demandInstitution: string | null;
  closeDate: string | null;
  estimatedPrice: number | null;
  noticeUrl: string | null;
  status: BidStatus;
  syncedAt: string;
  requirements: RequirementItem[];
}

export interface RequirementInput {
  category: string;
  description: string;
  requiredValue: string;
  unit: string | null;
  mandatory: boolean;
}

export interface ProductSpecDetail {
  specId: number;
  specKey: string;
  specValue: string;
  unit: string | null;
}

export interface Product {
  productId: number;
  productName: string;
  category: string;
  specs: ProductSpecDetail[];
}

export interface ProductInput {
  productName: string;
  category: string;
  specs: { specKey: string; specValue: string; unit: string | null }[];
}

export interface MatchItem {
  requirementId: number;
  category: string;
  description: string;
  status: MatchStatusLabel;
  requiredValue: string;
  productValue: string | null;
  mandatory: boolean;
  note: string;
}

export interface MatchSummary {
  totalCount: number;
  satisfied: number;
  partial: number;
  unsatisfied: number;
  unknown: number;
  /** 충족률(%). 부분충족은 0.5건으로 계산된다. */
  satisfactionRate: number;
  mandatoryUnsatisfied: number;
  /** 필수 요구사항에 미충족이 없으면 true */
  biddable: boolean;
}

export interface MatchResult {
  bidId: number;
  bidTitle: string;
  productId: number;
  productName: string;
  matchedAt: string | null;
  results: MatchItem[];
  summary: MatchSummary;
}

export interface SyncReport {
  fetched: number;
  created: number;
  updated: number;
  failedKeywords: string[];
}

/** 백엔드 공통 에러 응답 */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
