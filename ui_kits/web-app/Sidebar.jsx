// JivRas — Sidebar (Inventory Dashboard)
// Load: <script type="text/babel" src="Sidebar.jsx"></script>

const SIDEBAR_CATEGORIES = [
  { icon: '🌾', label: 'Grains & Pulses', count: 8 },
  { icon: '🫙', label: 'Oils & Fats', count: 5 },
  { icon: '🥛', label: 'Dairy', count: 3 },
  { icon: '🍯', label: 'Sweeteners', count: 4 },
  { icon: '🌿', label: 'Spices', count: 6 },
  { icon: '🥜', label: 'Nuts & Seeds', count: 7 },
];

const SIDEBAR_NAVS = [
  { icon: '📦', label: 'Inventory' },
  { icon: '📊', label: 'Reports' },
  { icon: '👤', label: 'Create Role' },
];

const Sidebar = ({ activeNav, setActiveNav, activeCat, setActiveCat }) => {
  const stats = [
    { val: '24', lbl: 'Products', warn: false },
    { val: '3', lbl: 'Low Stock', warn: true },
  ];

  return (
    <aside style={{
      width: 220, minWidth: 220, background: '#D57937',
      display: 'flex', flexDirection: 'column',
      overflowY: 'auto', overflowX: 'hidden', position: 'relative',
    }}>
      {/* Subtle bg circle */}
      <div style={{ position: 'absolute', bottom: -60, right: -60, width: 200, height: 200, borderRadius: '50%', background: 'rgba(255,255,255,0.04)', pointerEvents: 'none' }}></div>

      {/* Brand */}
      <div style={{ padding: '22px 18px 16px', fontFamily: 'Syne, sans-serif', fontSize: 15, fontWeight: 700, color: '#fff', letterSpacing: '0.04em', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
        <span style={{ color: '#fff' }}>Jiv</span><span style={{ color: 'rgba(255,255,255,0.75)' }}>Ras</span>
        <span style={{ opacity: 0.55, fontWeight: 400, display: 'block', fontSize: 10, marginTop: 2, letterSpacing: '0.1em', textTransform: 'uppercase' }}>Inventory</span>
      </div>

      {/* Main Nav */}
      <div style={{ padding: '8px 0 4px' }}>
        {SIDEBAR_NAVS.map(nav => (
          <div key={nav.label}
            onClick={() => setActiveNav(nav.label)}
            style={{
              display: 'flex', alignItems: 'center', gap: 9, padding: '10px 18px',
              cursor: 'pointer', fontSize: 13.5, fontWeight: activeNav === nav.label ? 600 : 500,
              color: activeNav === nav.label ? '#fff' : 'rgba(255,255,255,0.65)',
              background: activeNav === nav.label ? 'rgba(255,255,255,0.14)' : 'transparent',
              position: 'relative', transition: 'background 0.15s, color 0.15s',
            }}>
            {activeNav === nav.label && <div style={{ position: 'absolute', left: 0, top: '50%', transform: 'translateY(-50%)', width: 3, height: 22, background: '#D4820A', borderRadius: '0 2px 2px 0' }}></div>}
            <span style={{ fontSize: 14 }}>{nav.icon}</span>
            {nav.label}
          </div>
        ))}
      </div>

      {/* Categories */}
      {activeNav === 'Inventory' && (
        <>
          <div style={{ padding: '12px 18px 6px', fontSize: 9, letterSpacing: '0.12em', textTransform: 'uppercase', color: 'rgba(255,255,255,0.4)', fontFamily: 'Syne, sans-serif' }}>Categories</div>
          {SIDEBAR_CATEGORIES.map(cat => (
            <div key={cat.label}
              onClick={() => setActiveCat(cat.label)}
              style={{
                display: 'flex', alignItems: 'center', gap: 8, padding: '8px 18px',
                cursor: 'pointer', color: activeCat === cat.label ? '#fff' : 'rgba(255,255,255,0.65)',
                fontSize: 12.5, fontWeight: activeCat === cat.label ? 500 : 400,
                background: activeCat === cat.label ? 'rgba(255,255,255,0.14)' : 'transparent',
                transition: 'background 0.15s', position: 'relative',
              }}>
              {activeCat === cat.label && <div style={{ position: 'absolute', left: 0, top: '50%', transform: 'translateY(-50%)', width: 3, height: 18, background: '#8FC49E', borderRadius: '0 2px 2px 0' }}></div>}
              <span>{cat.icon}</span>
              <span style={{ flex: 1 }}>{cat.label}</span>
              <span style={{ fontSize: 10, background: 'rgba(255,255,255,0.12)', padding: '1px 6px', borderRadius: 10, color: 'rgba(255,255,255,0.7)' }}>{cat.count}</span>
            </div>
          ))}
        </>
      )}

      {/* Quick Stats */}
      <div style={{ padding: '12px 16px 16px', display: 'flex', flexDirection: 'column', gap: 8, marginTop: 'auto' }}>
        {stats.map(s => (
          <div key={s.lbl} style={{ background: 'rgba(255,255,255,0.08)', borderRadius: 8, padding: '8px 12px' }}>
            <div style={{ fontSize: 17, fontWeight: 700, color: s.warn ? '#EFC94C' : '#fff', fontFamily: 'Syne, sans-serif' }}>{s.val}</div>
            <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.45)', marginTop: 1 }}>{s.lbl}</div>
          </div>
        ))}
      </div>
    </aside>
  );
};

Object.assign(window, { Sidebar, SIDEBAR_CATEGORIES });
