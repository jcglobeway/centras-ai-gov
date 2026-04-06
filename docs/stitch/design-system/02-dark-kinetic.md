# Design System Strategy: The Kinetic Intelligence Framework

## 1. Overview & Creative North Star
**Creative North Star: The Obsidian Nerve Center**
This design system is engineered for the high-stakes environment of AI operations. It rejects the "bubbly" consumer SaaS aesthetic in favor of a sophisticated, technical interface that feels like a high-performance instrument. We move beyond a standard dashboard by treating the UI as a living ecosystem of data. 

The system achieves an "Editorial Technical" look through **intentional density**. We do not fear high information density; we master it through extreme typographic discipline and tonal layering. The layout is anchored by a rigid 240px sidebar but breathes through asymmetrical content distribution in the 12-column grid, allowing complex RAG (Retrieval-Augmented Generation) flows to feel structured yet fluid.

---

## 2. Colors: Tonal Architecture
The color palette is built on a "Deep Sea" foundation of navy and cobalt. We move away from the "flat" look by using a spectrum of dark neutrals to define space.

### The "No-Line" Rule
**Borders are a failure of hierarchy.** In this system, 1px solid borders for sectioning are strictly prohibited. You must define boundaries through background shifts:
- Place a `surface-container-low` component on a `surface` background.
- Use `surface-container-highest` for active or focused states.
- Separation is achieved through the **color shift**, not a stroke.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers.
- **Base Level:** `surface` (#0b1326) — The "floor" of the application.
- **Secondary Level:** `surface-container-low` (#131b2e) — Large content areas.
- **Tertiary Level:** `surface-container` (#171f33) — Cards and modules.
- **Active/Hover Level:** `surface-container-high` (#222a3d) — Interactive zones.

### The Glass & Gradient Rule
To prevent the UI from feeling stagnant, main CTAs and "AI-active" states should utilize a subtle gradient:
- **Primary Gradient:** Linear 135° from `primary-container` (#2563eb) to `secondary-container` (#33467e).
- **Glassmorphism:** For floating menus or command palettes, use `surface-container` at 80% opacity with a `20px` backdrop-blur.

---

## 3. Typography: The Technical Monolith
The typographic system creates a tension between the human-readable (`Inter`) and the machine-processed (`IBM Plex Mono`).

*   **Display & Headlines (`Inter`):** Use `headline-sm` (1.5rem) for main page titles. Keep tracking tight (-0.02em) to maintain a premium, editorial feel.
*   **The "Data Layer" (`IBM Plex Mono`):** All numeric metrics, LLM tokens, latency values, and code snippets *must* use IBM Plex Mono. This distinguishes "raw data" from "system labels."
*   **The Label Hierarchy:** Use `label-sm` (0.6875rem) in all-caps with +0.05em letter spacing for metadata headers. This creates a "Control Room" aesthetic.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are too "soft" for this technical system. We use **Ambient Depth**.

*   **The Layering Principle:** Depth is achieved by stacking. A `surface-container-lowest` card placed inside a `surface-container-high` section creates a recessed, "etched" look.
*   **Ambient Shadows:** If a card must float, use a tinted shadow: `0px 8px 24px rgba(0, 0, 0, 0.4)`. Avoid grey shadows; ensure the shadow inherits the deep navy of the background to maintain color saturation.
*   **The "Ghost Border" Fallback:** If accessibility requires a container edge, use `outline-variant` (#434655) at **15% opacity**. It should be felt, not seen.

---

## 5. Components: Precision Primitives

### Buttons
*   **Primary:** `primary-container` (#2563eb) background. No border. `DEFAULT` radius (0.25rem).
*   **Secondary:** `surface-container-highest` background. Text in `on-surface`.
*   **Tertiary:** Ghost style. No background. `on-surface-variant` text. High-contrast blue text only on hover.

### Inputs & Search
*   **Technical Input:** Use `surface-container-lowest` with a 1px `outline-variant` at 20% opacity. 
*   **Focus State:** Shift background to `surface-container` and change border to `primary` (#b4c5ff) at 50% opacity.

### Cards & Data Lists
*   **Zero-Divider Policy:** Never use a horizontal line to separate list items. Use 0.4rem (`spacing-2`) of vertical padding and a subtle hover state shift to `surface-container-low`.
*   **RAG Trace Visualizers:** Use `IBM Plex Mono` for the trace IDs. Status indicators use a 6px solid circle (not a chip) of the status color (`#10b981`, etc.) next to the text.

### The "Pulse" Component (Signature)
For AI processing states, use a 2px horizontal bar at the top of the container that animates a gradient crawl between `primary` and `secondary`. This provides a non-intrusive "living" indicator.

---

## 6. Do’s and Don’ts

### Do:
*   **Do** use `IBM Plex Mono` for all numbers. It aligns decimals better and looks more authoritative.
*   **Do** embrace density. Use `spacing-2` and `spacing-3` for tight technical clusters.
*   **Do** use "Surface-on-Surface" transitions to define the 240px sidebar vs. the content area.

### Don't:
*   **Don't** use pure black (#000000) or pure white (#FFFFFF). Always use the themed neutrals.
*   **Don't** use large corner radii. Stick to `sm` (0.125rem) and `md` (0.375rem) to keep the "Technical" edge. Large rounds are for consumer apps.
*   **Don't** use icons without labels in the sidebar. Precision requires clarity.