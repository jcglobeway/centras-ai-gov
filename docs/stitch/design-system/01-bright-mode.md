# Design System Specification: The Technical Precision Framework

## 1. Overview & Creative North Star: "The Digital Architect"
This design system is built for high-stakes, data-dense B2B environments where clarity is paramount and cognitive load must be minimized. Our Creative North Star is **"The Digital Architect"**—a philosophy that treats UI not as a collection of boxes, but as a structured, layered blueprint. 

To move beyond the "generic SaaS" look, we reject the standard rigid grid in favor of **intentional tonal layering**. By utilizing sophisticated background shifts instead of heavy borders, we create a layout that feels expansive and professional. The experience should feel like a high-end physical dashboard: precise, authoritative, and frictionless.

## 2. Colors & Surface Philosophy
The palette is rooted in a technical "Slate" foundation, punctuated by a high-performance "Signal Blue."

### Surface Hierarchy & The "No-Line" Rule
Traditional UIs rely on 1px solid borders to separate sections. In this system, **borders are a last resort.** We define boundaries through background color shifts and nesting tiers.
*   **Base Layer:** Use `surface` (#f8f9ff) for the main application canvas.
*   **The Sidebar:** Use `surface_container_lowest` (#ffffff). Do not use a high-contrast border; use a 0.5px `outline_variant` at 20% opacity or simply let the tonal shift against the `surface` background define the edge.
*   **Nesting Logic:** 
    *   `surface` (Background) → `surface_container_low` (Section) → `surface_container_lowest` (Card).
    *   This "stacking" creates natural depth without visual clutter.

### The Glass & Gradient Rule
To provide a premium "soul" to technical data:
*   **CTAs:** Use a subtle linear gradient on `primary` buttons, transitioning from `primary` (#004ac6) to `primary_container` (#2563eb) at a 135-degree angle.
*   **Floating Panels:** Use `surface_container_lowest` with an 80% opacity and a `20px` backdrop-blur to create a "frosted glass" effect for dropdowns and modals.

| Token | Value | Role |
| :--- | :--- | :--- |
| `primary` | #004ac6 | Actionable high-priority elements. |
| `surface` | #f8f9ff | The primary application backdrop. |
| `surface_container_low` | #eff4ff | Sub-sections and grouping containers. |
| `on_surface_variant` | #434655 | Secondary labels and metadata. |
| `tertiary` | #943700 | Warm accents for high-attention technical alerts. |

## 3. Typography: The Editorial Contrast
We utilize a dual-typeface system to distinguish between **Interface Guidance** and **Raw Data**.

*   **Inter (UI Labels):** Used for all navigation, headers, and instructional text. It provides a neutral, highly readable "voice."
*   **IBM Plex Mono (Metrics):** Used exclusively for numeric data, timestamps, and code snippets. The monospaced nature communicates technical precision and "Datadog-level" accuracy.

**Scale Highlights:**
*   **Display-sm (Inter, 2.25rem):** Reserved for high-level dashboard summaries.
*   **Title-sm (Inter, 1rem, Medium):** Standard header for cards and sections.
*   **Label-sm (IBM Plex Mono, 0.6875rem):** Micro-data, status timestamps, and technical metadata.

## 4. Elevation & Depth: Tonal Layering
We move away from the "drop shadow" era into **Tonal Stacking**.

*   **The Layering Principle:** Depth is achieved by placing a "bright" surface on a "dim" background. A `surface_container_lowest` (#ffffff) card sitting on a `surface_container_low` (#eff4ff) background creates an automatic, soft lift.
*   **Ambient Shadows:** If a shadow is required (e.g., a floating modal), use a triple-layered shadow:
    *   `0px 4px 20px rgba(11, 28, 48, 0.04)`
    *   `0px 2px 8px rgba(11, 28, 48, 0.04)`
*   **Ghost Borders:** For accessibility in data tables, use `outline_variant` (#c3c6d7) at **15% opacity**. This provides a "hint" of a line without breaking the editorial flow.

## 5. Components: Technical Primitives

### Cards & Data Lists
*   **No Dividers:** Prohibit the use of horizontal rules (`<hr>`). Separate list items using `spacing-4` (0.9rem) or alternating background tints between `surface_container_lowest` and `surface_container_low`.
*   **Corner Radius:** Use `md` (0.375rem) for cards to maintain a "technical/sharp" feel. Avoid overly rounded "bubbly" corners.

### Buttons & Chips
*   **Primary Action:** `primary` background with `on_primary` text. No border.
*   **Secondary Action:** `surface_container_high` background. No border. Text in `primary`.
*   **Technical Chips:** Use `IBM Plex Mono` for text within chips. Use `surface_variant` for the background to denote "read-only" status data.

### Input Fields
*   **Structure:** Minimalist. Only the bottom border is visible in the default state using `outline_variant` (20% opacity). On focus, the border transitions to `primary` and grows to 2px.
*   **Micro-copy:** All helper text must use `label-sm` in `on_surface_variant`.

### Technical Status Indicators
*   **Success:** `Green #10b981` (Surface tint for icons/pills).
*   **Warning:** `Amber #f59e0b` (Surface tint for icons/pills).
*   **Critical:** `Red #ef4444` (Surface tint for icons/pills).
*   *Implementation:* Use a 10% opacity background of the color with a 100% opacity icon for a "soft-signal" look.

## 6. Do's and Don'ts

### Do
*   **Do** use `IBM Plex Mono` for any number that can change (counters, prices, IDs).
*   **Do** favor vertical whitespace over horizontal lines.
*   **Do** use "nested" surfaces (`low` inside `base`) to group related content.
*   **Do** use `backdrop-filter: blur()` on all overhanging or sticky elements.

### Don't
*   **Don't** use pure black (#000000) for text. Use `on_background` (#0b1c30) to maintain tonal softness.
*   **Don't** use 100% opaque, 1px borders for layout sectioning.
*   **Don't** use "Inter" for data tables; it lacks the technical rhythm required for scanning rows of numbers.
*   **Don't** use standard box-shadows; if it looks like a "drop shadow," it's too heavy.