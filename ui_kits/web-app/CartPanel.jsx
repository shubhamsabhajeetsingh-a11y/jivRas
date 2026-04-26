// JivRas — CartPanel (Storefront)
// Load: <script type="text/babel" src="CartPanel.jsx"></script>

const CartPanel = ({ items, setItems, onCheckout }) => {
  const total = items.reduce((s, i) => s + i.price * i.qty, 0);

  const changeQty = (id, delta) => {
    setItems(prev => prev.map(i => i.id === id ? { ...i, qty: Math.max(0, i.qty + delta) } : i).filter(i => i.qty > 0));
  };

  if (items.length === 0) return (
    <div style={{ textAlign: 'center', padding: '60px 20px', background: '#fff', borderRadius: 16, boxShadow: '0 4px 15px rgba(0,0,0,0.08)' }}>
      <div style={{ fontSize: 48, marginBottom: 12, opacity: 0.4 }}>🛒</div>
      <h3 style={{ fontFamily: 'Syne, sans-serif', color: '#333', margin: '0 0 8px' }}>Your cart is empty</h3>
      <p style={{ color: '#888', fontSize: 13 }}>Add some fresh groceries to get started.</p>
    </div>
  );

  return (
    <div style={{ maxWidth: 640, margin: '0 auto' }}>
      <div style={{ background: '#fff', borderRadius: 16, boxShadow: '0 4px 15px rgba(0,0,0,0.08)', overflow: 'hidden', marginBottom: 16 }}>
        {items.map((item, i) => (
          <div key={item.id} style={{ display: 'flex', alignItems: 'center', padding: '16px 20px', borderBottom: i < items.length - 1 ? '1px solid #f0f0f0' : 'none' }}>
            <div style={{ fontSize: 28, marginRight: 14 }}>{item.emoji}</div>
            <div style={{ flex: 1 }}>
              <div style={{ fontFamily: 'Syne, sans-serif', fontWeight: 600, fontSize: 14, color: '#1A1A1A' }}>{item.name}</div>
              <div style={{ fontSize: 12, color: '#7A7A7A', marginTop: 2 }}>₹{item.price}/kg</div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '0 20px' }}>
              <button onClick={() => changeQty(item.id, -1)} style={qtyBtnStyle}>−</button>
              <span style={{ fontWeight: 600, fontSize: 13, minWidth: 40, textAlign: 'center', color: '#1A1A1A' }}>{item.qty} kg</span>
              <button onClick={() => changeQty(item.id, 1)} style={qtyBtnStyle}>+</button>
            </div>
            <div style={{ fontFamily: 'Syne, sans-serif', fontWeight: 700, fontSize: 14, color: '#1A1A1A', minWidth: 70, textAlign: 'right' }}>₹{(item.price * item.qty).toFixed(0)}</div>
          </div>
        ))}
      </div>
      <div style={{ background: '#fff', borderRadius: 16, boxShadow: '0 4px 15px rgba(0,0,0,0.08)', padding: 22 }}>
        {[['Subtotal', `₹${total.toFixed(0)}`], ['Delivery', '₹40'], ['Total', `₹${(total + 40).toFixed(0)}`]].map(([k, v], i) => (
          <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '7px 0', borderTop: i === 2 ? '2px solid #eee' : 'none', marginTop: i === 2 ? 8 : 0, fontWeight: i === 2 ? 700 : 400, fontSize: i === 2 ? 16 : 13, color: i === 2 ? '#1A1A1A' : '#666' }}>
            <span>{k}</span>
            <span style={{ color: i === 2 ? '#D57937' : '#1A1A1A', fontFamily: i === 2 ? 'Syne, sans-serif' : 'inherit' }}>{v}</span>
          </div>
        ))}
        <button onClick={onCheckout} style={{ width: '100%', padding: 14, background: 'linear-gradient(135deg, #6c5ce7, #a55eea)', color: '#fff', border: 'none', borderRadius: 12, fontSize: 15, fontWeight: 700, cursor: 'pointer', marginTop: 16, fontFamily: 'Syne, sans-serif' }}>
          Proceed to Checkout →
        </button>
      </div>
    </div>
  );
};

const qtyBtnStyle = { width: 30, height: 30, border: '2px solid #E2DDD6', borderRadius: '50%', background: '#fff', cursor: 'pointer', fontSize: 16, fontWeight: 700, color: '#1A1A1A', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 0 };

Object.assign(window, { CartPanel });
