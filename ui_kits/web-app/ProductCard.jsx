// JivRas — ProductCard (Storefront)
// Load: <script type="text/babel" src="ProductCard.jsx"></script>

const ProductCard = ({ product, onAdd, added }) => {
  const isLow = product.stock < 10;
  return (
    <div style={{
      background: '#fff', borderRadius: 16, overflow: 'hidden',
      boxShadow: '0 10px 20px rgba(0,0,0,0.08)',
      display: 'flex', flexDirection: 'column',
      transition: 'transform 0.25s ease, box-shadow 0.25s ease',
    }}
      onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-8px)'; e.currentTarget.style.boxShadow = '0 16px 32px rgba(0,0,0,0.13)'; }}
      onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 10px 20px rgba(0,0,0,0.08)'; }}
    >
      <div style={{ height: 160, background: product.imgBg || '#F5F3EE', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 52, position: 'relative' }}>
        {product.emoji}
        <span style={{
          position: 'absolute', top: 10, right: 10, fontSize: 11, fontWeight: 700,
          padding: '3px 9px', borderRadius: 6, letterSpacing: '0.04em', textTransform: 'uppercase',
          fontFamily: 'Syne, sans-serif',
          background: isLow ? '#FDECEA' : '#D6F0DD',
          color: isLow ? '#C0392B' : '#1E7A35',
        }}>
          {isLow ? 'Low Stock' : 'In Stock'}
        </span>
      </div>
      <div style={{ padding: 18, display: 'flex', flexDirection: 'column', flexGrow: 1 }}>
        <h3 style={{ margin: '0 0 4px', fontSize: 15, fontFamily: 'Syne, sans-serif', fontWeight: 700, color: '#1A1A1A' }}>{product.name}</h3>
        <p style={{ margin: '0 0 12px', fontSize: 12, color: '#7A7A7A', lineHeight: 1.4, flexGrow: 1 }}>{product.description}</p>
        <div style={{ marginBottom: 12 }}>
          <span style={{ background: isLow ? '#FEF3DC' : '#E8F0EA', color: isLow ? '#D4820A' : '#1E7A35', padding: '3px 10px', borderRadius: 4, fontSize: 12, fontWeight: 600 }}>
            Stock: {product.stock} kg
          </span>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid #f0f0f0', paddingTop: 14 }}>
          <span style={{ fontSize: 18, fontWeight: 700, color: '#1A1A1A', fontFamily: 'Syne, sans-serif' }}>₹{product.price} <small style={{ fontSize: 11, fontWeight: 400, color: '#7A7A7A' }}>/kg</small></span>
          <button
            onClick={() => onAdd(product)}
            disabled={added || product.stock <= 0}
            style={{
              background: added ? '#27ae60' : '#D57937',
              color: '#fff', border: 'none', padding: '8px 18px',
              borderRadius: 8, cursor: added ? 'default' : 'pointer',
              fontWeight: 700, fontSize: 12, fontFamily: 'Syne, sans-serif',
              transition: 'all 0.2s', opacity: product.stock <= 0 ? 0.5 : 1,
            }}>
            {added ? '✓ Added!' : 'Add to Cart'}
          </button>
        </div>
      </div>
    </div>
  );
};

Object.assign(window, { ProductCard });
