# AlphaPics AI — Current UI Audit

Date: 2026-08-27

## Audit scope

This combined UX, visual-design, and screenshot-level accessibility audit covers the current production Compose surfaces captured by the existing Pixel 8 Roborazzi suite:

1. Home — initial viewport (`app/src/test/screenshots/greeting.png`)
2. Home — tools viewport (`app/src/test/screenshots/home_tools.png`)
3. Enhance — unavailable state (`app/src/test/screenshots/enhance.png`)
4. Compress — empty state (`app/src/test/screenshots/compressor.png`)
5. Batch — empty state (`app/src/test/screenshots/batch.png`)
6. History — empty state (`app/src/test/screenshots/history.png`)
7. Settings (`app/src/test/screenshots/settings.png`)

The target user goal is to choose or capture a photo, improve it with minimal friction, refine it when needed, and explicitly save or share a trustworthy result. The accessibility target is an Android phone experience with readable hierarchy, practical touch targets, meaningful semantics, and layouts that remain usable with text scaling.

## Existing strengths

- The black/navy, electric-blue, violet, and cyan system already reads as a coherent AlphaPics identity.
- Home clearly brands the product as a photo enhancer and editor. Forbidden generation and video destinations are absent.
- The flagship action is visually stronger than secondary tools.
- Compression, Batch, History, and Settings already share recognizable surface, border, type, and icon conventions.
- Unavailable enhancement processing is not misrepresented as functional.
- Compression exposes Gallery and Camera distinctly and retains explicit Target Size and Quality modes.
- Batch states the 20-photo limit directly.
- Major visible icon buttons are generally at least 48 dp.

## UX risks tied to the captured steps

### 1. Home — initial viewport

Health: structurally promising, visually oversized.

- The hero occupies most of the first viewport without showing a real user photo or a persistent Gallery/Camera choice. The result is photo-themed rather than photo-first.
- The large headline, generous gaps, and tall hero push the four core shortcuts and Quick Tools far below the fold.
- “Enhance Photo” opens a generic unavailable screen, so the dominant action does not lead into the requested enhancement workspace architecture.
- The logo, title, subtitle, hero label, headline, supporting copy, upload motif, and CTA create too many stacked hierarchy levels before the user reaches a concrete choice.

### 2. Home — tools viewport

Health: clear categories, too card-heavy.

- Four equal feature cards visually compete with the flagship enhancer instead of acting as compact shortcuts.
- The Edit Photo entry is strong but too far down the page.
- Quick Tools are appropriately limited to three, but the overall scroll distance makes them feel buried.
- Several large bordered surfaces create a dashboard impression despite the focused tool count.

### 3. Enhance — unavailable state

Health: honest, but a dead end.

- Honest “Coming Soon” copy is a trust strength.
- The page does not preserve the user’s photo context, show the intended canvas, or teach the future Before/After and tool-rail model.
- Returning home is the only action, so it cannot serve as the production shell for future enhancement processing.

### 4. Compress — empty state

Health: functional and trustworthy, moderately dense.

- The separate Gallery and Camera entry is immediately understandable.
- Target Size and Quality are explicit and avoid hidden automation.
- The screenshot shows the start of the Target Size card cut off, which signals a long form and weak above-the-fold prioritization.
- Original and Compressed metric cards are visible before a photo is selected, consuming space with placeholder values.
- The control density is acceptable for a professional utility but should retain consistent 12/16/24 dp rhythm.

### 5. Batch — empty state

Health: safe and clear, spatially inefficient.

- The 20-photo limit and Add Photos action are explicit.
- The large empty-state card plus unused lower viewport make the screen feel unfinished.
- The empty state could more usefully explain that existing compression settings and token rules remain in force.

### 6. History — empty state

Health: understandable, vertically unbalanced.

- Copy explains how history is populated.
- The empty card sits low in the viewport with a large dead zone above it, weakening visual balance and discoverability.
- A compact “Compress a photo” action would provide recovery without changing persistence behavior.

### 7. Settings

Health: complete and consistent, larger than necessary.

- Grouping into Appearance, Support, Legal, and About is conventional and scannable.
- Large rows and repeated 44 dp framed glyphs make the page feel more decorative than utility-focused.
- The custom text chevron should be replaced by a standard icon for consistent optical weight and semantics.

## Screenshot-visible accessibility risks

- Secondary text is frequently low-contrast blue-grey on navy. Contrast must be measured in implementation, particularly `TextTertiary` on `SurfaceRaised`.
- All horizontal rails need semantic labels, selected-state announcements, and a non-color selection signal.
- Slider values must expose readable names and current values, not only position.
- Text scaling can increase wrapping in Home shortcuts, tool rails, Settings rows, and contextual panels; those surfaces must avoid fixed heights around multi-line copy.
- Before/After controls require a labeled draggable handle plus alternate Before and After buttons for users who cannot drag precisely.
- Loading, error, and success state changes need screen-reader announcements in the future processing implementation.
- Screenshot evidence cannot confirm TalkBack order, switch access, focus behavior, contrast ratios, reduced-motion behavior, or dynamic-type resilience; those remain implementation checks.

## Highest-impact opportunities

1. Make the first Home viewport an immediate photo entry with visible Gallery and Camera actions.
2. Convert the enhancement dead end into a real, photo-dominant workspace shell that remains explicit about unavailable processing.
3. Replace equal Home feature cards with a compact shortcut rail and one clearly dominant editor entry.
4. Introduce one shared contextual-workspace model for enhancer and editor: minimal top bar, canvas, mode rail, contextual panel, explicit Cancel/Apply or Save.
5. Compress empty-state layouts in Batch and History while keeping their existing engines and persistence untouched.
6. Standardize state, slider, thumbnail, rail, toolbar, and badge primitives in the AlphaPics presentation package.

## Evidence limits

This is a screenshot-grounded audit of captured states, not a full WCAG compliance claim. Camera, Gallery, permissions, compression execution, ads, Save to Gallery, Share, database migration, token gating, and batch execution require code and runtime verification in addition to visual evidence.
