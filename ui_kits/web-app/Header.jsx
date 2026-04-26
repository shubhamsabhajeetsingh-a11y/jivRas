// JivRas — Header (Storefront)
// Load: <script type="text/babel" src="Header.jsx"></script>

const Header = ({ screen, setScreen, cartCount = 0, isLoggedIn = false }) => {
  return (
    <header style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      background: 'rgba(255,255,255,0.96)', padding: '14px 28px',
      borderRadius: 12, marginBottom: 24,
      boxShadow: '0 4px 15px rgba(0,0,0,0.08)',
    }}>
      <div style={{ fontFamily: 'Syne, sans-serif', fontSize: 20, fontWeight: 700, letterSpacing: '0.02em', cursor: 'pointer' }}
        onClick={() => setScreen('products')}>
        <span style={{ color: '#2E6B35' }}>Jiv</span><span style={{ color: '#E8920A' }}>Ras</span>
        <span style={{ fontSize: 11, fontWeight: 500, color: '#7A7A7A', marginLeft: 8, letterSpacing: '0.1em', textTransform: 'uppercase', fontFamily: 'DM Sans, sans-serif' }}>Natural</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        {isLoggedIn && (
          <button onClick={() => setScreen('my-orders')} style={hdrBtnStyles('#F5F3EE', '#D57937', '#D57937')}>📦 My Orders</button>
        )}
        <button onClick={() => setScreen('cart')} style={{ ...hdrBtnStyles('#D57937', '#fff'), position: 'relative' }}>
          🛒 Cart
          {cartCount > 0 && (
            <span style={{ position: 'absolute', top: -6, right: -6, background: '#C0392B', color: '#fff', fontSize: 10, fontWeight: 700, width: 18, height: 18, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{cartCount}</span>
          )}
        </button>
        {isLoggedIn ? (
          <div style={{ width: 32, height: 32, borderRadius: '50%', background: '#D57937', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 13, fontWeight: 700, fontFamily: 'Syne, sans-serif', cursor: 'pointer' }}>U</div>
        ) : (
          <button onClick={() => setScreen('login')} style={hdrBtnStyles('#1A1A1A', '#fff')}>Login</button>
        )}
      </div>
    </header>
  );
};

const hdrBtnStyles = (bg, color, borderColor) => ({
  background: bg, color: color,
  border: borderColor ? `1px solid ${borderColor}` : 'none',
  padding: '8px 18px', borderRadius: 25, cursor: 'pointer',
  fontWeight: 700, fontSize: 13, fontFamily: 'DM Sans, sans-serif',
  transition: 'all 0.2s', position: 'relative',
});

Object.assign(window, { Header });
