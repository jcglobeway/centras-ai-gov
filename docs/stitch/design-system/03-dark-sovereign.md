# Design System Specification: The Command Aesthetic

## 1. Overview & Creative North Star
**Creative North Star: The Sovereign Intelligence**
This design system moves beyond the generic "SaaS dashboard" to create a high-density, authoritative environment. It is inspired by brutalist technical documentation and aerospace control interfaces—prioritizing raw data clarity over decorative fluff. 

The system achieves a "High-End Editorial" feel not through ornamentation, but through **hyper-precision**. By pairing the humanist neutrality of *Inter* with the industrial, fixed-width rhythm of *IBM Plex Mono*, we create a tension between human readability and machine-grade accuracy. We break the traditional "flat" grid by utilizing extreme tonal depth and intentional white space to guide the eye through complex datasets without the need for noisy structural lines.

---

## 2. Colors: Tonal Architecture
The palette is rooted in deep space navies and cool slates, designed to minimize eye strain during long-duration monitoring.

### Surface Hierarchy & Nesting
Forget "boxes." View the UI as a series of receding planes.
*   **Base Layer:** `surface` (#0b1326) – The foundation.
*   **Sectional Layer:** `surface_container_low` (#131b2e) – Used for large layout blocks.
*   **Actionable Layer:** `surface_container` (#171f33) – The standard card or container.
*   **Prominent Layer:** `surface_container_highest` (#2d3449) – For popovers or nested data pods.

### The "No-Line" Rule
**Explicit Instruction:** Do not use `1px solid` borders for sectioning layout areas. Structural definition must be achieved through background shifts. For example, a `surface_container_low` sidebar should sit directly against a `surface` main content area. The eye perceives the edge through the shift in hex value, creating a cleaner, more premium interface.

### The "Glass & Gradient" Signature
While the base is technical, main CTAs and "Hero" metrics utilize a **Subtle Kinetic Gradient**. Transitioning from `primary_container` (#2563eb) to a slightly deeper shift provides a "lit-from-within" feel that signifies the energy of active AI processing.

---

## 3. Typography: The Dual-Engine System
Typography is our primary tool for information hierarchy. We use two distinct typefaces to separate "Narrative" from "Data."

*   **Inter (UI & Narrative):** Used for all headings, body copy, and UI instructions. It is approachable and ensures the platform feels like a modern service.
*   **IBM Plex Mono (The Data Engine):** Used for all numeric values, metric labels, status badges, and code snippets. This "Monospace-as-Premium" approach signals technical rigor and prevents "character jump" when numbers update in real-time.

**Scale Highlights:**
*   **Display-LG (Inter):** 3.5rem – Used for high-level system status or hero statements.
*   **Title-SM (Inter):** 1rem – The standard card heading.
*   **Label-MD (IBM Plex Mono):** 0.75rem – Used for all table headers and axis labels.

---

## 4. Elevation & Depth: Tonal Layering
We reject the heavy drop-shadows of the 2010s. Depth is earned, not applied.

*   **The Layering Principle:** To lift an element, move it up the `surface_container` scale. A "floating" modal should use `surface_container_highest` (#2d3449).
*   **Ambient Shadows:** For floating elements (modals, dropdowns), use a wide-dispersion shadow: `0px 20px 40px rgba(0,0,0,0.4)`. The shadow must feel like a soft atmospheric occlusion, not a hard line.
*   **The Ghost Border:** For accessibility in form fields, use the `outline_variant` (#434655) at 20% opacity. This provides a "hint" of a container without breaking the seamless tonal flow.
*   **Backdrop Blur:** Floating menus must use a `12px` backdrop-blur with a semi-transparent `surface_container` color to create a "Frosted Graphite" effect, allowing underlying data to be sensed but not distracting.

---

## 5. Components: Precision Primitives

### Buttons
*   **Primary:** `primary_container` (#2563eb) background. 6px radius. Label in `on_primary_container` (#eeefff). Apply a 2px inner-glow on top edge for a "tactile" feel.
*   **Tertiary/Ghost:** No background. `primary` text. Use for secondary actions to maintain layout "density."

### Input Fields
*   **Base State:** `surface_container_lowest` (#060e20) background, 4px radius. 
*   **Border:** Use the "Ghost Border" (1px `outline_variant` at 20%).
*   **Focus:** Border becomes 1px solid `primary` (#b4c5ff) with a soft 2px outer glow.

### Cards & Lists
*   **Constraint:** Zero dividers. Use vertical spacing (Scale `5` or `6`) to separate list items. 
*   **Interaction:** On hover, a list item should shift from `surface` to `surface_container_low`. 

### AI Status Badges (The "Blink" Component)
*   A specialized component for this system. A small 6px dot using status colors (`tertiary` for Success, `error` for Critical) with a soft 4px outer glow of the same color to simulate a hardware LED.

---

## 6. Do’s and Don’ts

### Do
*   **Do** use IBM Plex Mono for every single digit in the UI. Consistency in numbers is non-negotiable.
*   **Do** leverage asymmetry. If a dashboard has three columns, make one 2x the width of the others to create an editorial focus.
*   **Do** use "Breathing Room." Even in a "dense" technical UI, use Spacing Scale `10` (2.25rem) between major sections to prevent cognitive overload.

### Don’t
*   **Don’t** use pure black (#000000) for shadows; always use a tinted navy or transparent `on_background` variant.
*   **Don’t** use icons as the primary way to convey meaning. Pair icons with `label-sm` text for an authoritative, "labeled-switch" look.
*   **Don’t** use 100% opaque borders to separate the sidebar from the main content. Use the tonal shift between `surface` and `surface_container_low`.