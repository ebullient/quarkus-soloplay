# SCSS Structure

This directory contains the SCSS source files that compile to CSS at build time.

## Directory Structure

```
src/main/scss/
├── _variables.scss       # Color scheme, spacing, typography variables
├── _mixins.scss          # Reusable SCSS mixins
├── base/
│   ├── _reset.scss       # CSS reset and body styles
│   └── _typography.scss  # Typography styles
├── components/
│   ├── _badges.scss      # Status badges, character types
│   ├── _buttons.scss     # Button styles (primary, secondary, AI, etc.)
│   ├── _flash.scss       # Flash messages (success/error)
│   ├── _forms.scss       # Form inputs and campaign selector
│   ├── _header.scss      # Page headers
│   └── _nav.scss         # Navigation bars
├── layouts/
│   ├── _chat.scss        # Chat interface layout
│   ├── _ingest.scss      # Document ingestion layout
│   └── _inspector.scss   # Campaign inspector layout
├── styles.scss           # Main stylesheet (imports all base + components)
├── chat.scss             # Chat-specific entry point
├── ingest.scss           # Ingest-specific entry point
└── inspector.scss        # Inspector-specific entry point
```

## Build Process

SCSS files are automatically compiled to CSS during Maven's `generate-resources` phase:

- Input: `src/main/scss/*.scss`
- Output: `target/classes/META-INF/resources/*.css`

The `sass-cli-maven-plugin` (configured in `pom.xml`) handles compilation.

## Color Scheme

The application uses **semantic color naming** to support light/dark mode theming.

### Background Colors (Layered)
- `--color-bg-base`: #1a1a2e (main background)
- `--color-bg-elevated`: #0f0f1e (elevated surfaces)
- `--color-bg-overlay`: #16213e (overlays, dialogs)

### Text Colors (Semantic Roles)
- `--color-text-primary`: #e4e4e4 (main text)
- `--color-text-secondary`: #999 (muted text)
- `--color-text-accent`: #d4af37 (gold - primary accent)
- `--color-text-accent-bright`: #f4d03f (brighter accent)

### AI Agent Colors
- `--color-text-ai`: #667eea (AI primary)
- `--color-text-ai-accent`: #764ba2 (AI secondary)

### Chat Participant Colors
- `--color-bg-user`: #1e3a5f (user message background)
- `--color-text-user`: #4a90e2 (user accent)
- `--color-bg-assistant`: #2d1e1e (assistant background)
- `--color-text-assistant`: #d4af37 (assistant accent)

### Status Colors
- `--color-success`, `--color-success-text`, `--color-success-bg`
- `--color-error`, `--color-error-text`, `--color-error-bg`

## Variables System

This project uses **semantic CSS custom properties** for easy light/dark mode support:

### CSS Custom Properties (Runtime Theming)

Defined on `:root` in `base/_base.scss`. Change these to switch themes:

```css
:root {
  --color-bg-base: #1a1a2e;
  --color-text-primary: #e4e4e4;
  --color-text-accent: #d4af37;
  --spacing-md: 1rem;
  /* ...and many more */
}
```

### SCSS Variables (Compile-time Only)

Defined in `_variables.scss` - **only** for values that don't benefit from runtime theming:

```scss
// Font families - used in font stacks
$font-family-serif: 'Georgia', serif;
$font-family-mono: 'Courier New', monospace;

// Transitions - animation timing
$transition-fast: 0.2s;
$transition-base: 0.3s;

// Scrollbar - browser-specific
$scrollbar-width: 10px;
$scrollbar-width-sm: 8px;
```

**All other values** (colors, spacing, font sizes, border radius) use `var()` directly - **no SCSS variables needed**!

### Benefits of This Approach

✅ **Runtime theming** - Change colors/spacing via JavaScript
✅ **Browser DevTools** - Inspect and modify CSS variables in real-time
✅ **SCSS convenience** - Use familiar SCSS syntax throughout
✅ **Performance** - CSS variables are native and fast

### Example: Runtime Theme Change

```javascript
// Change the entire gold theme to blue at runtime
document.documentElement.style.setProperty('--color-gold', '#4a9eff');
document.documentElement.style.setProperty('--color-gold-light', '#6eb5ff');
```

## Utility Classes

Composable layout primitives defined in `components/_utilities.scss`:

**Layout:**
- `.card` - Elevated surface with padding and border
- `.stack` - Vertical flex layout with gap (variants: `.stack-sm`, `.stack-lg`)
- `.cluster` - Horizontal flex layout with gap
- `.cluster-between` - Horizontal flex with space-between
- `.grid` - Responsive auto-fill grid (variant: `.grid-sm`)

**Width:**
- `.wide` - Max-width 800px, centered
- `.narrow` - Max-width 600px, centered
- `.center` - Centered flex container

**Usage Example:**
```html
<div class="grid">
  <article class="card stack">
    <header class="cluster-between">
      <h3>Title</h3>
      <span>Badge</span>
    </header>
    <p>Content...</p>
  </article>
</div>
```

## Mixins

Defined in `_mixins.scss` (use sparingly, prefer utility classes):

- `@include custom-scrollbar` - Styled scrollbars
- `@include status-message($bg, $border, $text)` - Flash messages
- `@include badge($bg, $text)` - Badge styling
- `@include gradient-bg($color1, $color2, $angle)` - Linear gradients

## Entry Points

Four main SCSS entry points compile to separate CSS files:

1. **styles.scss** → `styles.css` - Base styles used by all pages
2. **chat.scss** → `chat.css` - Chat interface specific styles
3. **ingest.scss** → `ingest.css` - Document ingestion specific styles
4. **inspector.scss** → `inspector.css` - Campaign inspector specific styles

Templates include the appropriate CSS files based on their needs.

## Development

When running `./mvnw quarkus:dev`, Quarkus watches for SCSS changes and automatically recompiles.

To manually compile SCSS:
```bash
./mvnw generate-resources
```

## Migration from Plain CSS

The original CSS files in `src/main/resources/META-INF/resources/` have been refactored into this SCSS structure for better maintainability with:

- Shared variables (no more hardcoded colors)
- Reusable mixins (DRY principle)
- Modular organization (easier to find and update styles)
- Nested selectors (more readable)
