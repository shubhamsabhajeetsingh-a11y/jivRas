# JivRas Design System

**JivRas Natural** is a natural & organic grocery brand based in Nalasopara, India. Their tagline is *"Essence of Life, Born from the Soil."* The product range includes cold-pressed oils, jaggery powder, peanut butter, desi cow ghee, and other farm-sourced staples.

## Sources

- **GitHub Repo**: `shubhamsabhajeetsingh-a11y/jivRas` (Angular 17 + Spring Boot)
- **Background/Brand Image**: `assets/background.jpeg` — product hero with logo
- No Figma link was provided.

## Products / Surfaces

| Surface | Description |
|---|---|
| **Customer Storefront** (`/products`) | Browse & buy groceries; blurred background image, card grid, search |
| **Cart + Checkout** (`/cart`, `/checkout`) | Order flow; delivery address, Razorpay payment |
| **Inventory Dashboard** (`/inventory-dashboard`) | Employee/admin tool; sidebar + product grid/table |
| **Auth** (`/login`, `/create-users`, `/create-employee`) | Login, registration |
| **My Orders** (`/my-orders`) | Customer order history |

---

## CONTENT FUNDAMENTALS

### Voice & Tone
- **Warm, natural, trustworthy.** Copy feels grounded and earthy — like a farmer's market, not a supermarket.
- **Brand name is always "JivRas"** — "Jiv" in green, "Ras" in orange in the logo. Never all-caps, never all-lowercase.
- **Tagline**: *"Essence of Life, Born from the Soil"* — always sentence-case, always in italics when used inline.

### Casing
- Page headings: Title Case ("My Orders", "JivRas Groceries")
- UI labels and nav items: Title Case
- Table column headers: ALL CAPS, tight letter-spacing (Syne font)
- Descriptions: Sentence case

### Pronoun Style
- **You-first**: "Your cart", "Your orders", "Deliver to you"
- Not first-person brand copy (no "We believe…")

### Emoji Usage
- **Emoji are used sparingly in the customer storefront** (🛒 Cart, 📦 My Orders) for friendliness.
- The inventory dashboard uses **no emoji** — clean professional tone.
- Emoji are used as product category icons in the inventory sidebar (e.g. 🌾 Grains, 🥛 Dairy).

### Numbers & Pricing
- Indian Rupee symbol: ₹ (always prefix, no space) — e.g. `₹120`
- Weight: kg (lowercase, no period)
- Prices per kg: `₹120 /kg`

---

## VISUAL FOUNDATIONS

### Color System
The design has **two layers** — a refined internal dashboard system and a legacy customer storefront system (in transition):

#### Dashboard (Refined — use this as source of truth)
| Token | Hex | Usage |
|---|---|---|
| `--bg` | `#F5F3EE` | Page background — warm off-white, earthy |
| `--surface` | `#FFFFFF` | Cards, modals, panels |
| `--accent` | `#D57937` | Primary brand orange — buttons, sidebar bg, active states |
| `--amber` | `#D4820A` | Secondary warm amber — alerts, highlights |
| `--amber-light` | `#FEF3DC` | Amber tint backgrounds |
| `--red` | `#C0392B` | Danger, low-stock, delete actions |
| `--red-light` | `#FDECEA` | Red tint backgrounds |
| `--text` | `#1A1A1A` | Primary body text |
| `--text-muted` | `#7A7A7A` | Secondary/placeholder text |
| `--border` | `#E2DDD6` | Borders, dividers — warm grey |
| `--green` | `#1E7A35` | Success, in-stock status |
| `--green-light` | `#D6F0DD` | Success tint backgrounds |
| `--accent-light` | `#E8F0EA` | Light green tint (accent success) |
| `--sidebar-indicator` | `#8FC49E` | Active nav indicator in sidebar |

#### Logo Colors (Brand Core)
| Name | Hex | Usage |
|---|---|---|
| JivRas Green | `#2E6B35` | "Jiv" wordmark, leaf icon |
| JivRas Orange | `#E8920A` | "Ras" wordmark, sun rays |

#### Customer Storefront (Legacy — being refactored)
- Background: blurred product photo (`assets/background.jpeg`)
- Purple `#6C5CE7` used on cart/checkout — likely to be replaced with accent orange
- Gradient: `linear-gradient(135deg, #6c5ce7, #a55eea)` — CTAs

### Typography
**Two typefaces:**
- **Syne** (Google Fonts) — Display, headings, brand labels, nav, buttons. Weights: 400, 500, 600, 700.
- **DM Sans** (Google Fonts) — Body, input fields, descriptions. Weights: 300, 400, 500.

| Role | Font | Size | Weight | Notes |
|---|---|---|---|---|
| Page title | Syne | 19px | 700 | Topbar title |
| Section title | Syne | 15px | 700 | With dot indicator |
| Card name | Syne | 14px | 600 | Product card heading |
| Metric value | Syne | 18px | 700 | Stat chips |
| Nav item | DM Sans | 14px | 500 | Sidebar nav |
| Body copy | DM Sans | 13-14px | 400 | General text |
| Labels (caps) | Syne | 10-11px | 700 | ALL CAPS, 0.07-0.12em spacing |
| Badges/chips | Syne/DM Sans | 10-11px | 500-600 | Status pills |
| Input text | DM Sans | 13-14px | 400 | Form fields |

### Spacing & Layout
- Base unit: **8px**
- Card padding: `14px`
- Section padding: `24px 28px`
- Sidebar width: `220px`
- Card border-radius: `14px` (product cards), `16px` (modals/panels), `8px` (inputs, buttons)
- Input border-radius: `8-10px`

### Backgrounds
- Dashboard: flat warm off-white `#F5F3EE`
- Customer storefront: full-bleed background photo with `blur(8px)` + `scale(1.1)` — product/nature imagery
- Cards/panels: white surfaces on warm bg
- Sidebar: solid `--accent` (#D57937) background

### Borders & Shadows
- Border: `1px solid var(--border)` — `#E2DDD6` warm grey
- Card hover shadow: `0 8px 24px rgba(0,0,0,0.07)`
- Panel shadow: `0 16px 40px rgba(0,0,0,0.05)`
- Modal shadow: `0 24px 48px rgba(0,0,0,0.2)`
- Input focus: `0 0 0 3px rgba(212,130,10,0.12)` — amber glow

### Corner Radii
- Large cards/modals: `14–16px`
- Buttons (primary): `8–10px`
- Pill buttons (storefront): `25px` (fully rounded)
- Small badges: `6–10px`
- Stat chips (sidebar): `8px`

### Animation
- Card hover: `translateY(-3px)` + shadow — `0.2s ease`
- Button hover: `opacity: 0.88` or `translateY(-1px)` — `0.15–0.2s`
- Modal entry: `slideUp` — `translateY(20px)→0`, `0.25s ease-out`
- Toast: `slideIn` from right, `0.3s ease`
- Spinner: rotate 360°, `0.7s linear infinite`
- Dropdown: `translateY(-6px)→0`, `0.15s ease-out`
- No bounces — easing is smooth/linear throughout

### Hover & Press States
- Primary buttons: `opacity: 0.88` on hover; `translateY(-1px)` on some CTAs
- Outline buttons: fill with `var(--accent)` background on hover, text goes white
- Nav items: `rgba(255,255,255,0.08)` background on hover (on orange sidebar)
- Table rows: `var(--bg)` background on hover
- Card products: `translateY(-3px)` lift + shadow

### Active/Focus States
- Inputs: `border-color: var(--accent)` + amber glow ring
- Active nav: `rgba(255,255,255,0.14)` bg + 3–4px left border indicator
  - Main nav: amber `#D4820A` indicator
  - Category items: green `#8FC49E` indicator

### Imagery & Photography
- Warm, natural tones — earthy greens, golden ambers, wood textures
- Products photographed on natural surfaces (wood, burlap)
- Blurred nature background used on storefront (soft-focus green)
- No illustrations — photography-only

### Cards
- White surface on `#F5F3EE` bg
- `1px solid #E2DDD6` border
- `14px` border-radius
- Hover: `translateY(-3px)` + soft shadow `0 8px 24px rgba(0,0,0,0.07)`
- Product image: `110px` height, object-fit cover
- Status badge: absolute top-right, uppercase pill

### Use of Transparency & Blur
- Sidebar overlay circle: `rgba(255,255,255,0.04)` subtle depth
- Modal overlay: `rgba(0,0,0,0.5)`
- Storefront bg: `blur(8px)` on background image
- Nav items on sidebar: white with alpha (`rgba(255,255,255,0.65)`)
- Scrollbar thumb: `rgba(255,255,255,0.25)` on sidebar

---

## ICONOGRAPHY

- **No dedicated icon font or SVG sprite** exists in the codebase.
- The inventory dashboard uses **emoji as icons** for nav items (🌾 Grains, 🥛 Dairy, etc.) and empty states.
- The storefront uses emoji in headers (🛒, 📦).
- **No Lucide/Heroicons/Material Icons** are used — inline SVG is used in the search bar only.
- For new designs, recommend: **Lucide Icons** (CDN: `https://unpkg.com/lucide@latest`) — matches the stroke-based, clean aesthetic.
- Brand logo uses a plant/sunburst icon (see `assets/background.jpeg`).

---

## FILE INDEX

```
README.md                     — This file
SKILL.md                      — Agent skill definition
colors_and_type.css           — All CSS custom properties (tokens)
assets/
  logo.svg                    — Enhanced SVG logo (vector, scalable, inline-ready)
  logo.png                    — Original brand logo photograph
  background.jpeg             — Brand hero image with logo
  favicon.ico                 — Browser favicon
preview/
  logo.html                   — Logo on light/dark/brand backgrounds + wordmark
  colors-brand.html           — Brand + logo colors
  colors-semantic.html        — Semantic UI colors (surface, bg, border)
  colors-status.html          — Status colors (green/amber/red)
  type-display.html           — Syne display/heading specimens
  type-body.html              — DM Sans body/label specimens
  type-scale.html             — Full type scale
  spacing-tokens.html         — Spacing & radius tokens
  shadows.html                — Shadow & elevation system
  buttons-primary.html        — Primary + outline buttons
  buttons-states.html         — Button states (hover, disabled, loading)
  inputs.html                 — Form inputs & focus states
  badges.html                 — Status badges & pills
  cards.html                  — Product card component
  sidebar.html                — Sidebar navigation component
  topbar.html                 — Topbar / header component
  modals.html                 — Modal & overlay component
ui_kits/
  web-app/
    README.md
    index.html                — Interactive prototype
    Header.jsx
    ProductCard.jsx
    Sidebar.jsx
    CartPanel.jsx
```
