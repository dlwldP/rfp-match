import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { BidDetailPage } from './pages/BidDetailPage';
import { BidListPage } from './pages/BidListPage';
import { ProductListPage } from './pages/ProductListPage';

export default function App() {
  return (
    <>
      <header className="app-header">
        <div className="app-header__inner">
          <div className="app-header__brand">
            RFP-스펙 매칭
            <span>보안장비 공공입찰 갭분석</span>
          </div>
          <nav className="app-nav">
            <NavLink to="/bids" className={({ isActive }) => (isActive ? 'active' : '')}>
              입찰공고
            </NavLink>
            <NavLink to="/products" className={({ isActive }) => (isActive ? 'active' : '')}>
              제품 스펙
            </NavLink>
          </nav>
        </div>
      </header>

      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/bids" replace />} />
          <Route path="/bids" element={<BidListPage />} />
          <Route path="/bids/:bidId" element={<BidDetailPage />} />
          <Route path="/products" element={<ProductListPage />} />
          <Route path="*" element={<Navigate to="/bids" replace />} />
        </Routes>
      </main>
    </>
  );
}
