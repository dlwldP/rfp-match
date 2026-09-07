import { useCallback, useEffect, useState } from 'react';
import { productApi, toErrorMessage } from '../api/client';
import type { Product, ProductInput } from '../api/types';

interface SpecRow {
  specKey: string;
  specValue: string;
  unit: string;
}

const EMPTY_SPEC: SpecRow = { specKey: '', specValue: '', unit: '' };

const emptyForm = (): { productName: string; category: string; specs: SpecRow[] } => ({
  productName: '',
  category: '',
  specs: [{ ...EMPTY_SPEC }],
});

export function ProductListPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setProducts(await productApi.list());
    } catch (err) {
      setError(toErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const startEdit = (product: Product) => {
    setEditingId(product.productId);
    setForm({
      productName: product.productName,
      category: product.category,
      specs: product.specs.map((spec) => ({
        specKey: spec.specKey,
        specValue: spec.specValue,
        unit: spec.unit ?? '',
      })),
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm(emptyForm());
  };

  const submit = async () => {
    const specs = form.specs
      .filter((spec) => spec.specKey.trim() && spec.specValue.trim())
      .map((spec) => ({
        specKey: spec.specKey.trim(),
        specValue: spec.specValue.trim(),
        unit: spec.unit.trim() ? spec.unit.trim() : null,
      }));

    if (!form.productName.trim() || !form.category.trim() || specs.length === 0) {
      setError('제품명, 제품군, 스펙 1건 이상은 필수입니다.');
      return;
    }

    const payload: ProductInput = {
      productName: form.productName.trim(),
      category: form.category.trim(),
      specs,
    };

    setError(null);
    try {
      if (editingId) {
        await productApi.update(editingId, payload);
      } else {
        await productApi.create(payload);
      }
      cancelEdit();
      await load();
    } catch (err) {
      setError(toErrorMessage(err));
    }
  };

  const remove = async (productId: number) => {
    setError(null);
    try {
      await productApi.remove(productId);
      if (editingId === productId) cancelEdit();
      await load();
    } catch (err) {
      setError(toErrorMessage(err));
    }
  };

  const updateSpec = (index: number, patch: Partial<SpecRow>) => {
    setForm((prev) => ({
      ...prev,
      specs: prev.specs.map((spec, i) => (i === index ? { ...spec, ...patch } : spec)),
    }));
  };

  return (
    <div className="page">
      <h1 className="page__title">자사 제품 스펙</h1>
      <p className="page__subtitle">
        매칭의 기준이 되는 제품 카탈로그입니다. 항목명은 RFP 문구와 달라도 됩니다 — 매칭 엔진이 동의어를 맞춰 줍니다
        (예: "처리성능" ↔ "네트워크 처리량").
      </p>

      {error && <div className="alert alert--error">{error}</div>}

      <section className="card">
        <h2 className="card__title">
          {editingId ? '제품 수정' : '제품 등록'}
          <span className="card__hint">CC인증 등급은 보유 인증서 기준으로 입력하세요</span>
        </h2>

        <div className="toolbar">
          <input
            placeholder="제품명 (예: NGIPS-2000)"
            value={form.productName}
            onChange={(event) => setForm((prev) => ({ ...prev, productName: event.target.value }))}
            style={{ minWidth: 240 }}
          />
          <input
            placeholder="제품군 (예: IPS)"
            value={form.category}
            onChange={(event) => setForm((prev) => ({ ...prev, category: event.target.value }))}
          />
        </div>

        <div className="row-grid row-grid--spec row-grid__head">
          <div>스펙 항목</div>
          <div>값</div>
          <div>단위</div>
          <div />
        </div>
        {form.specs.map((spec, index) => (
          <div className="row-grid row-grid--spec" key={index}>
            <input
              placeholder="처리성능 / CC인증등급 / 국정원검증필"
              value={spec.specKey}
              onChange={(event) => updateSpec(index, { specKey: event.target.value })}
            />
            <input
              placeholder="20 / EAL4 / YES"
              value={spec.specValue}
              onChange={(event) => updateSpec(index, { specValue: event.target.value })}
            />
            <input
              placeholder="Gbps"
              value={spec.unit}
              onChange={(event) => updateSpec(index, { unit: event.target.value })}
            />
            <button
              className="link danger"
              disabled={form.specs.length === 1}
              onClick={() =>
                setForm((prev) => ({
                  ...prev,
                  specs: prev.specs.length === 1 ? prev.specs : prev.specs.filter((_, i) => i !== index),
                }))
              }
            >
              행 삭제
            </button>
          </div>
        ))}

        <div className="toolbar" style={{ marginTop: 12, marginBottom: 0 }}>
          <button onClick={() => setForm((prev) => ({ ...prev, specs: [...prev.specs, { ...EMPTY_SPEC }] }))}>
            + 스펙 행 추가
          </button>
          <button className="primary" onClick={() => void submit()}>
            {editingId ? '수정 저장' : '제품 등록'}
          </button>
          {editingId && <button onClick={cancelEdit}>취소</button>}
          {editingId && <span className="muted">수정하면 이 제품의 기존 갭분석 결과는 초기화됩니다.</span>}
        </div>
      </section>

      <section className="card">
        <h2 className="card__title">등록된 제품 <span className="card__hint">{products.length}종</span></h2>

        {loading && <div className="empty">불러오는 중…</div>}
        {!loading && products.length === 0 && <div className="empty">등록된 제품이 없습니다.</div>}

        {products.map((product) => (
          <div key={product.productId} style={{ borderBottom: '1px solid var(--grid)', padding: '14px 0' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
              <strong>{product.productName}</strong>
              <span className="badge badge--muted">{product.category}</span>
              <span style={{ marginLeft: 'auto', display: 'flex', gap: 4 }}>
                <button className="link" onClick={() => startEdit(product)}>수정</button>
                <button className="link danger" onClick={() => void remove(product.productId)}>삭제</button>
              </span>
            </div>
            <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {product.specs.map((spec) => (
                <span key={spec.specId} className="badge badge--muted">
                  {spec.specKey}: <span className="mono">{spec.specValue}{spec.unit ?? ''}</span>
                </span>
              ))}
            </div>
          </div>
        ))}
      </section>
    </div>
  );
}
