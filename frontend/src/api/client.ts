import axios from 'axios';
import type {
  BidDetail,
  BidSummary,
  MatchResult,
  PageResponse,
  Product,
  ProductInput,
  RequirementInput,
  RequirementItem,
  SyncReport,
} from './types';

const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

/** 백엔드 공통 에러 포맷의 message를 그대로 화면에 띄우기 위해 꺼낸다. */
export function toErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === 'string' && message.length > 0) {
      return message;
    }
    if (error.response?.status === 502) {
      return '입찰공고 Open API 연동에 실패했습니다. 마지막으로 수집한 데이터를 표시합니다.';
    }
    return error.message;
  }
  return '알 수 없는 오류가 발생했습니다.';
}

export const bidApi = {
  list: (params: { keyword?: string; status?: string; page?: number; size?: number }) =>
    client.get<PageResponse<BidSummary>>('/bids', { params }).then((res) => res.data),

  detail: (bidId: number) => client.get<BidDetail>(`/bids/${bidId}`).then((res) => res.data),

  sync: () => client.post<SyncReport>('/bids/sync').then((res) => res.data),
};

export const requirementApi = {
  register: (bidId: number, items: RequirementInput[]) =>
    client.post<{ bidId: number; requirementIds: number[] }>(`/bids/${bidId}/requirements`, { items })
      .then((res) => res.data),

  list: (bidId: number) =>
    client.get<RequirementItem[]>(`/bids/${bidId}/requirements`).then((res) => res.data),

  remove: (bidId: number, requirementId: number) =>
    client.delete(`/bids/${bidId}/requirements/${requirementId}`).then(() => undefined),
};

export const productApi = {
  list: () => client.get<Product[]>('/products').then((res) => res.data),

  detail: (productId: number) => client.get<Product>(`/products/${productId}`).then((res) => res.data),

  create: (input: ProductInput) => client.post<Product>('/products', input).then((res) => res.data),

  update: (productId: number, input: ProductInput) =>
    client.put<Product>(`/products/${productId}`, input).then((res) => res.data),

  remove: (productId: number) => client.delete(`/products/${productId}`).then(() => undefined),
};

export const matchApi = {
  run: (bidId: number, productId: number) =>
    client.post<MatchResult>(`/bids/${bidId}/match`, null, { params: { productId } }).then((res) => res.data),

  result: (bidId: number, productId: number) =>
    client.get<MatchResult>(`/bids/${bidId}/match-result`, { params: { productId } }).then((res) => res.data),
};
