# AlphaPics AI — Production UI/UX Specification

Version: 1.0

Date: 2026-08-27

Platform: native Android, Kotlin, Jetpack Compose, Material 3

Product definition: premium AI Photo Enhancer + advanced Photo Editor. Photo only.

## 1. Design intent

AlphaPics should feel like a focused mobile photo studio, not a dashboard. The photo, its result, and the next meaningful action must dominate every core workflow. Dark surfaces create a cinematic viewing environment; electric blue identifies primary action; violet and cyan distinguish selected modes and status without producing a rainbow of equal accents.

The design has three product layers:

1. **Start** — Home provides immediate Gallery and Camera entry plus a few compact destinations.
2. **Improve** — Enhancement and Editor workspaces center the photo and expose only the controls relevant to the selected tool.
3. **Finish** — Result, Compress, Batch, History, and explicit Save/Share surfaces make output status unambiguous.

No screen may imply that AI processing, removal, retouching, background replacement, sharpening, or export works when no corresponding engine exists.

## 2. Reference principles adopted

### UpFoto

- One obvious photo-first starting point.
- Gallery and Camera are immediate, concrete actions.
- Enhancement should be reachable with one primary decision, not through a feature dashboard.
- Restore, unblur, face, and upscale are modes of improving a photo, not independent destinations competing equally on Home.

### Remini

- The photo/result canvas is the primary surface.
- Before/After comparison is part of the result architecture, not a secondary settings page.
- Result controls are minimal and proof-oriented: Before, After, compare handle, Save, Share.
- Mode copy describes the visible result in plain language.

### PhotoRoom

- The editing workspace is contextual: canvas first, current tool second, all tools in a stable bottom rail.
- Batch is a dedicated workflow with an explicit item count, processing status, and completion summary.
- Saving/exporting is an intentional, named step with clear output state.
- Workspace tools do not flood Home.

### B612

- Bottom tool rails are compact, horizontally scrollable, and thumb-reachable.
- Sliders expose a label, current numeric value, reset affordance, and a generous touch target.
- Filter choices use visual thumbnails and a selected outline/label.
- Context panels use Cancel and Apply consistently.
- Advanced adjustments remain inside the active tool rather than becoming new screens.

AlphaPics does not reuse reference branding, assets, wording, gradients, or exact layout.

Reference sources inspected:

- UpFoto current official Google Play listing: https://play.google.com/store/apps/details?id=ai.photo.enhancer.photoclear&hl=en
- Remini official product page: https://remini.ai/
- PhotoRoom official product page: https://www.photoroom.com/
- B612 official product page: https://b612.snow.me/

## 3. Product boundaries and functional honesty

### In scope

- Photo selection and camera capture.
- Existing compression, batch compression, history, save, share, ads, and token flows.
- Photo-dominant enhancement workspace shell.
- Before/After result architecture that activates only when a real processed URI exists.
- Photo editor shell and safe in-session visual previews for controls that are truly implemented.
- Honest unavailable panels for processing that has no engine.

### Explicitly excluded

- AI image generation.
- Text-to-image.
- Avatars.
- Video generation.
- Image-to-video.
- Product generation.
- Any fake enhancement result, fake progress, fake object removal, fake retouching, or fake background cutout.

### Capability labels

Every user-visible tool is one of:

- **Available** — backed by a real existing processor and output path.
- **Preview only** — visibly changes the in-session canvas but cannot export an edited file; the screen states this before interaction.
- **Coming soon** — has no processing engine; controls are replaced by an honest explanation and no fake progress.

Current capability contract:

| Surface | Current capability |
| --- | --- |
| Compress — Target Size | Available |
| Compress — Quality | Available |
| Batch Compress | Available |
| Compression History | Available |
| Compression Save / Share | Available |
| Adjust preview | Preview only |
| Basic filter preview | Preview only |
| Crop / transform preview | Preview only |
| AI Enhance modes | Coming soon until a real processor is wired |
| Retouch | Coming soon |
| Remove / Magic Eraser | Coming soon |
| Background removal / replace | Coming soon |
| Detail / sharpen | Coming soon |
| Text overlay | Coming soon |
| Editor export | Coming soon; never reuse compression output as an edited result |

## 4. Navigation architecture

The production navigation model remains intentionally shallow:

```text
Home
├── Enhancement Workspace
│   └── Before/After Result (only with real result)
├── Photo Editor
│   ├── Adjust
│   ├── Retouch
│   ├── Remove
│   ├── Background
│   ├── Crop
│   ├── Filters
│   ├── Detail
│   └── Text
├── Compress
├── Batch
├── History
└── Settings
```

Android system Back returns to the prior product surface. Context-panel Cancel restores the values present when that panel opened. Apply commits values only to the current in-session preview. Export is never enabled unless a real output writer exists.

## 5. Visual system

### Color tokens

- Foundation / void: `#04050A`.
- Deep navy: `#070B16`.
- Primary surface: `#0D1425`.
- Raised surface: `#121B31`.
- Soft active surface: `#171F35`.
- Structural border: `#263453` at restrained opacity.
- Primary electric blue: `#3478FF`.
- Supporting bright blue: `#56A0FF`.
- Violet: `#9A5CFF`, used only for selection or secondary emphasis.
- Cyan: `#52E2FF`, used for compact status and focus cues.
- Primary text: `#F7F9FF`.
- Secondary text: `#A9B4CC`.
- Tertiary text should not fall below accessible contrast on its actual surface.
- Success: `#5DE0B8`; warning: `#FFB85C`; destructive/error: `#FF7185`.

### Gradient policy

- No full-screen rainbow gradients.
- The main CTA may use a controlled blue-to-violet or cyan-blue-violet blend.
- Hero and canvas backgrounds use near-solid navy with subtle tonal variation.
- Glow is limited to selected controls, the flagship CTA, or a faint canvas edge. No neon bloom behind body copy.

### Typography

- Platform sans is acceptable for the current dependency footprint.
- Display: 30–34 sp, black/extra-bold, tight line height, reserved for one Home statement.
- Screen title: 20–24 sp, bold/extra-bold.
- Tool title: 14–16 sp, semibold/bold.
- Body: 14–16 sp, 1.4–1.5 line-height equivalent.
- Compact labels: 10–12 sp, semibold/bold, uppercase only for short category/status labels.
- Numeric slider values use tabular-feeling alignment and a minimum 12 sp size.

### Spacing and shape

- Base spacing unit: 4 dp.
- Screen horizontal inset: 16–18 dp.
- Major vertical gaps: 24–32 dp; never 40 dp repeatedly within a mobile viewport.
- Contextual-control gaps: 8–12 dp.
- Primary touch target: at least 48 x 48 dp.
- Rails: 64–76 dp high plus safe-area inset.
- Cards: 16–22 dp radius; hero/canvas: 24–28 dp.
- Borders are 1 dp and low contrast by default; selected state may use 1.5 dp electric blue/cyan.

### Iconography

- Use one consistent Material icon family or the existing AlphaPics line-icon family.
- Never use emoji, text glyphs, improvised ASCII, or unlabelled decorative shapes as controls.
- Every icon-only control has a meaningful content description.

## 6. Home specification

### Goal

Get a user from app launch to a selected or captured photo within one obvious action while keeping Compress, Batch, History, Settings, and Edit Photo discoverable.

### Structure and priority

1. Compact 64–72 dp header: AlphaPics brand mark and wordmark left; Settings 48 dp action right.
2. Dominant AI Photo Enhancer hero, approximately 300–340 dp on a 393 dp-wide phone.
3. Inside the hero: one concise benefit statement, a photo-entry well, then two explicit buttons — Gallery primary, Camera secondary.
4. A compact four-item enhancement shortcut rail: Enhance, Restore, Upscale, Remove Background.
5. One horizontal Edit Photo entry, visually distinct but secondary to the hero.
6. Compact Quick Tools row: Compress, Batch, History.
7. Existing banner ad remains in its current bottom area and does not obscure navigation or controls.

### Home copy

- Eyebrow: “AI PHOTO ENHANCER”.
- Headline: “Bring every photo back to its best.”
- Supporting copy: “Choose a photo to improve clarity, detail and tone.”
- Gallery: “Choose photo”.
- Camera: “Camera”.
- Shortcuts use one or two words only.

### Behavior

- Tapping Gallery or Camera opens the existing platform flow and then lands in the requested workspace with the selected URI.
- Tapping the hero outside the buttons opens the empty Enhancement Workspace without pretending a photo is selected.
- Enhance shortcut opens the Enhancement Workspace.
- Restore, Upscale, and Remove Background open the same photo-first shell focused on that mode; unavailable processing is visibly labeled.
- Edit Photo opens the Editor shell.
- Quick Tools preserve existing functional routes.

### Responsive behavior

- At larger font scales, the shortcut rail scrolls horizontally rather than shrinking labels below readability.
- Home uses content scrolling; the flagship hero must not be clipped at a standard Pixel 8 viewport.

## 7. AI Enhancement Workspace

### Layout

1. Minimal top bar: Back, compact title, optional help/status; no logo card.
2. Canvas takes the largest available central region.
3. Empty canvas: real Gallery and Camera actions.
4. Selected-photo canvas: fit-center photo on deep navy/black, preserving full bounds; no forced crop.
5. Result canvas: same bounds plus Before/After segmented buttons and a draggable compare handle only when a real processed result exists.
6. Compact mode rail below the canvas: Auto, Face, Unblur, Denoise, Restore, Color, Light, Detail, Upscale.
7. Context strip states the selected mode and real availability.
8. Primary bottom action is enabled only when a real processor is available. Otherwise it reads “Enhancement coming soon” and is disabled or replaced by a clear status panel.

### Result architecture

- Default result view is After.
- “Before” and “After” buttons provide a non-drag alternative.
- Compare handle is at least 48 dp wide in semantics even if its visible rule is thin.
- Labels remain visible and do not cover important photo regions.
- Save and Share appear only for a real validated output URI.
- Result status names the applied mode and output dimensions where available.

### Loading

- The photo remains visible with a dark scrim.
- Center status: progress indicator, active mode name, and calm copy such as “Improving photo detail”.
- Back/cancel behavior must be defined by the future processing engine; fake percentages are forbidden.
- Ads must not masquerade as progress.

### Error

- Keep the source photo visible.
- Inline error panel states what failed and offers “Try again” only when a real retry can run; otherwise “Back to tools”.
- Never lose the selected URI because processing failed.

## 8. Photo Editor workspace

### Layout

1. Minimal top bar: Back, “Photo Editor”, preview/export status.
2. Large fit-center image canvas.
3. Contextual panel directly above the main tool rail.
4. Main bottom rail: Adjust, Retouch, Remove, Background, Crop, Filters, Detail, Text.
5. Navigation-bar inset sits below the rail; controls never extend under system gestures.

### Workspace status

- If no photo is selected, the canvas is an empty photo-entry state with Gallery and Camera.
- If preview-only controls are active, a persistent compact label reads “Preview only · edited export isn’t available yet”.
- An Export/Save action stays disabled or is replaced by an honest “Export coming soon” label until a real editor output writer exists.

### Contextual interaction model

- Selecting a tool swaps the panel without moving the canvas.
- Cancel restores values from when the tool opened.
- Apply commits to the current preview session only when that preview is actually rendered.
- Back with unapplied values prompts or discards according to a defined state rule; it must not silently save.
- A Reset control restores neutral values.

## 9. Adjust

### Controls

- Exposure: `-100…100`, default 0.
- Contrast: `-100…100`, default 0.
- Saturation: `-100…100`, default 0.
- Warmth: `-100…100`, default 0.
- Optional future controls: Highlights, Shadows, Whites, Blacks, Tint, Vibrance.

### Slider anatomy

- Label left, numeric value right.
- Track height 4 dp; thumb visual 18–20 dp inside a 48 dp semantic/touch region.
- Active fill electric blue; focused thumb may use cyan.
- Double-tap or Reset returns to 0 only if discoverability is supplemented by a visible Reset action.

### Current implementation contract

Only controls that visibly update the Compose canvas may be interactive. They are marked Preview only. No edited image file is created.

## 10. Retouch

### Future controls

- Natural preset first.
- Skin, texture, blemish, eye detail, and teeth controls grouped as portrait adjustments.
- Strength defaults low; labels avoid appearance judgement.

### Current honest state

No retouch sliders are shown as functional. The panel states that portrait retouching is coming soon and preserves the photo canvas.

## 11. Remove / Magic Eraser

### Future interaction

- Brush size slider.
- Remove/Restore brush toggle.
- Undo/Redo.
- Zoom/pan with brush-mode conflict handling.
- Explicit Apply/Cancel.

### Current honest state

No fake brush mask, fake object detection, or fake progress. The tool panel explains that object removal is not available in this build.

## 12. Background

### Future interaction

- Remove background.
- Keep original.
- Transparent, solid color, blur, or user-photo replacement once real segmentation exists.
- Checkerboard transparency preview uses a real visual asset/pattern and clear label.

### Current honest state

No cutout preview without real segmentation. The panel remains a Coming Soon state.

## 13. Crop / Transform

### Controls

- Aspect choices: Free, Original, 1:1, 4:5, 3:2, 16:9.
- Rotate 90°.
- Flip horizontal and vertical.
- Straighten slider `-45…45` when a real crop renderer/output path exists.

### Current implementation contract

Session preview may change aspect ratio, rotation, and flip only if the canvas visibly renders those changes. Apply commits to preview state, not an output file. The status label remains Preview only.

## 14. Filters

### Anatomy

- Horizontal thumbnail rail, approximately 64–72 dp thumbnails.
- First item is Original.
- Selected item uses a blue/cyan outline plus label weight; selection does not rely on color alone.
- Strength slider appears only after a non-original filter is selected.

### Current implementation contract

Only deterministic local color-matrix previews may be offered. No “AI” filter name or generated visual is permitted. Export remains unavailable.

## 15. Detail

### Future controls

- Structure, sharpness, clarity, grain/noise.
- High zoom is encouraged for precision.

### Current honest state

No sharpening or denoise preview is implied without a real image processor. The panel is Coming Soon.

## 16. Text

Text means a text overlay on a user photo, never text-to-image.

### Future controls

- Add text, font, size, color, alignment, background, opacity, transform.
- Text entry requires a visible keyboard-safe layout and clear Apply/Cancel.

### Current honest state

Text overlay is Coming Soon until a real render and export path exists.

## 17. Compress

- Preserve `ImageCompressionEngine`, `ImageCompressionProcessor`, `CompressionPolicy`, Target Size, Quality, output validation, transparency, EXIF/orientation, tokens, ads, MediaStore, Save, and Share.
- Keep Target Size and Quality as the only primary modes.
- Keep Gallery and Camera explicit.
- The selected photo or Before/After result is the largest visual block.
- Show metric cards after selection where practical; avoid letting empty placeholders outrank photo entry.
- Processing copy must describe compression, not enhancement.
- Result actions remain Save state and Share, backed by the validated output URI.

## 18. Batch

- Dedicated screen; never merge Batch into Home or the single-photo editor.
- Preserve `MAX_BATCH_ITEMS = 20` and all existing processors/accounting.
- Empty state: Add Photos primary, item limit, short explanation, compact guidance.
- Selected state: item grid/list, per-item status, remove action, shared settings, Process All.
- Processing state: real per-item progress/status only; no fake percentages.
- Result state: completed/failed/skipped totals, retained errors, explicit save/share rules already supported by the processor.

## 19. History

- Preserve Room schema, migrations, DAO behavior, and persisted records.
- Empty state should sit near the visual center with a compact explanation and a route to Compress.
- Populated row shows thumbnail where available, filename, timestamp, original/final size, mode, and target status.
- Share uses the persisted output URI only when available.
- Missing output URI must degrade to an honest unavailable action, never a crash.

## 20. Settings

- Preserve existing support, rate, share, privacy, terms, version, and appearance actions.
- Compact top bar and rows.
- Grouping remains Appearance, Support, Legal, About.
- Standard chevrons replace text glyphs.
- Theme state remains explicit; dynamic theming must not override AlphaPics identity by default.

## 21. Shared states

### Empty

- One meaningful icon, one title, one sentence, one recovery action at most.
- Avoid full-height cards and unexplained dead space.

### Loading

- Use indeterminate progress unless a real processor exposes measured progress.
- Keep the user’s photo and active task name visible.
- Do not block system Back without a safe reason.

### Error

- Title says what failed.
- Body says whether the original is safe.
- Action is Retry only when retry is real; otherwise choose another photo or return.
- Destructive red is reserved for the error cue, not the whole screen.

### Coming Soon

- Always says “Coming soon” or “Not available yet”.
- Keeps the photo/workspace context when possible.
- Does not show active sliders, progress, success, Save, or fabricated results.
- Offers a useful available alternative, such as Compress or Adjust preview, only when contextually appropriate.

### Success/result

- Requires a real output or committed in-session preview state.
- Shows explicit status, output details, and Save/Share availability.

## 22. Accessibility and ergonomics

- Minimum control target 48 dp; rail items may be visually smaller but retain 48 dp semantics.
- Content descriptions describe action, not icon appearance.
- Selected tabs/tools expose selected semantics and a non-color visual cue.
- Slider semantics expose name and numeric value.
- Before/After can be operated without dragging.
- Text and controls must remain readable at 1.3x font scale; key actions cannot be clipped at 200% text.
- Contrast is checked for all token/surface pairs, especially tertiary copy.
- Motion is short, functional, and removable; no pulsing glow behind text.
- Error/loading/result changes should use live-region semantics when real async processing is integrated.

## 23. Implementation architecture

- Keep all new presentation code under `com.example.ui.alphapics`.
- Prefer stateless content composables with state/callback parameters for Roborazzi and Compose tests.
- MainActivity owns only routing and the existing platform pickers needed to pass real URIs into workspaces.
- Do not move, duplicate, or reinterpret compression, batch, history, token, ads, media, or database business logic.
- Use immutable UI models for tool and mode catalogs.
- Avoid introducing a new framework, WebView, React, Flutter, or web runtime.

## 24. Regression protection contract

The implementation must not modify behavior in:

- `ImageCompressionEngine`.
- `ImageCompressionProcessor`.
- `CompressionPolicy`.
- Target Size or Quality processing.
- Output validation.
- Transparency preservation.
- EXIF/orientation handling.
- Batch processing and `MAX_BATCH_ITEMS = 20`.
- Room database, migrations, or history persistence.
- `DailyTokenManager` and token gating.
- Rewarded, interstitial, or banner ads.
- Camera and Gallery contracts.
- Permissions.
- Save to Gallery, Share, or MediaStore.
- `applicationId`, package identity, signing configuration, or manifest identity.

Protected-file hashes are captured before implementation and compared after verification.

## 25. Visual QA acceptance criteria

Each major Roborazzi state is judged at the Pixel 8 viewport for:

- Photo prominence.
- Flagship Enhance emphasis.
- Compact vertical density.
- Clear hierarchy and copy.
- 4/8/12/16/24 dp rhythm.
- Typography, wrapping, and truncation.
- Color/token restraint and contrast risk.
- Consistent icons and radii.
- Touch ergonomics and selected states.
- Honest capability labeling.

Required screenshot coverage:

- Home initial.
- Home tools if scrolling remains necessary.
- Enhancement empty/selected-photo shell.
- Enhancement honest unavailable state.
- Before/After contract state only when backed by a real result; otherwise explicitly documented as unreachable.
- Editor empty/selected-photo shell.
- Adjust panel.
- Filters panel.
- Crop/Transform panel.
- Retouch, Remove, Background, Detail, and Text Coming Soon panels.
- Compress empty.
- Batch empty.
- History empty.
- Settings.
- Shared loading/error/empty/Coming Soon components where used.

No visual QA pass is accepted with actionable P0, P1, or P2 issues. P3 polish may remain documented.
