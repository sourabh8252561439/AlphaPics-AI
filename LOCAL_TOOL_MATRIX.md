# AlphaPics AI — Local Tool Matrix

> Audited against the production source on 2026-08-29. A control is `WORKING` only
> when it has a real local result, preview where applicable, full-resolution export,
> offline operation, error handling, and test evidence. UI-only controls are not
> counted as working. Update this matrix after every verified implementation phase.

| Tool | Status | Implementation File | Preview | Export | Undo/Redo | Offline | Tests | Known Limitation |
|---|---|---|---|---|---|---|---|---|
| Exposure | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Pixel + bitmap tests | Multiplicative ±2-stop range |
| Brightness | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Pixel + bitmap tests | Global brightness |
| Contrast | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Pixel + bitmap tests | Global midpoint contrast |
| Highlights | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Luminance-selectivity test | Smooth luminance mask |
| Shadows | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Luminance-selectivity test | Smooth luminance mask |
| Whites | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Pixel + bitmap tests | Smooth upper-range mask |
| Blacks | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Pixel + bitmap tests | Smooth lower-range mask |
| Gamma | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Endpoint + midtone test | Global nonlinear gamma |
| Saturation | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Desaturation test | Global saturation |
| Vibrance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Adaptive vibrance test | Protects already-saturated and neutral pixels |
| Temperature | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Temperature bias test | Manual temperature control |
| Tint | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Pixel + bitmap tests | Manual green/magenta control |
| White Balance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Pixel + bitmap tests | Manual temperature/tint; no eyedropper |
| HSL | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | HSL pixel + mapping tests | Eight smoothly overlapping hue families |
| Color Mix | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | RGB channel-isolation tests | Three-channel gain mixer; no cross-channel 3x3 matrix |
| Split Toning | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Shadow/highlight isolation tests | Two-way shadow/highlight toning with adjustable balance |
| Color Grading | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Tonal-region isolation + mapping tests | Independent shadows, midtones, and highlights; compact slider UI rather than wheels |
| HSL Red — Hue | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + red target tests | Maximum shift ±30° |
| HSL Red — Saturation | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Red target/isolation test | Smooth neighboring-channel falloff |
| HSL Red — Luminance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Red target/isolation test | Maximum shift ±35% |
| HSL Orange — Hue | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±30° |
| HSL Orange — Saturation | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Smooth neighboring-channel falloff |
| HSL Orange — Luminance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±35% |
| HSL Yellow — Hue | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±30° |
| HSL Yellow — Saturation | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Smooth neighboring-channel falloff |
| HSL Yellow — Luminance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±35% |
| HSL Green — Hue | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±30° |
| HSL Green — Saturation | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Smooth neighboring-channel falloff |
| HSL Green — Luminance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±35% |
| HSL Cyan/Aqua — Hue | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±30° |
| HSL Cyan/Aqua — Saturation | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Smooth neighboring-channel falloff |
| HSL Cyan/Aqua — Luminance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±35% |
| HSL Blue — Hue | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±30° |
| HSL Blue — Saturation | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Smooth neighboring-channel falloff |
| HSL Blue — Luminance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±35% |
| HSL Purple — Hue | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±30° |
| HSL Purple — Saturation | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Smooth neighboring-channel falloff |
| HSL Purple — Luminance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±35% |
| HSL Magenta — Hue | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±30° |
| HSL Magenta — Saturation | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Smooth neighboring-channel falloff |
| HSL Magenta — Luminance | WORKING | `editor/EditorPixelEngine.kt` | Yes | Yes | Yes | Yes | Generic HSL + enum mapping tests | Maximum shift ±35% |
| Master RGB Curve | WORKING | `editor/CurveEngine.kt` | Yes | Yes | Yes | Yes | LUT + pixel tests | 16-point maximum |
| Red Curve | WORKING | `editor/CurveEngine.kt` | Yes | Yes | Yes | Yes | Channel-isolation test | 16-point maximum |
| Green Curve | WORKING | `editor/CurveEngine.kt` | Yes | Yes | Yes | Yes | Shared channel/LUT tests | 16-point maximum |
| Blue Curve | WORKING | `editor/CurveEngine.kt` | Yes | Yes | Yes | Yes | Shared channel/LUT tests | 16-point maximum |
| Curve add control point | WORKING | `editor/CurveEngine.kt` | Yes | Yes | Yes | Yes | Curve editing test | Tap; points remain ordered |
| Curve drag point | WORKING | `editor/CurveEngine.kt` | Yes | Yes | Yes | Yes | Curve editing test | Endpoints retain fixed X positions |
| Curve delete point | WORKING | `editor/CurveEngine.kt` | Yes | Yes | Yes | Yes | Curve editing test | Double-tap; endpoints cannot be deleted |
| Curve reset | WORKING | `editor/CurveEngine.kt` | Yes | Yes | Yes | Yes | Curve + session tests | Per-channel and category reset |
| Curve smooth interpolation | WORKING | `editor/CurveEngine.kt` | Yes | Yes | Yes | Yes | Monotonic S-curve test | Monotone cubic Hermite LUT |
| Curve real-time preview | WORKING | `editor/EditorBitmapRenderer.kt` | Yes | Yes | Yes | Yes | Bitmap + screenshot tests | Preview capped at 1440 px; export is full resolution |
| Curve Undo/Redo | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session tests | Checkpoint created on Apply |
| Curve full-resolution export | WORKING | `editor/EditorExportManager.kt` | Yes | Yes | Yes | Yes | Renderer + export-path tests | Explicit failure if device cannot allocate full resolution |
| Sharpen | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Edge-contrast + bitmap tests | Local high-pass sharpen; 0..100 |
| Structure | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Local-result + block-parity tests | Broad-radius local contrast |
| Clarity | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Local-result + block-parity tests | Luminance-local contrast |
| Texture | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Local-result + block-parity tests | Fine-radius high pass |
| Noise Reduction | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Impulse suppression + block-parity tests | Edge-protected local denoise; UI label is Denoise |
| Dehaze | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Tone/color separation test | Deterministic tonal dehaze; no depth estimation |
| Vignette | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Center/corner isolation test | Supports dark and light edge finish |
| Grain | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Determinism test | Stable monochrome procedural grain |
| Fade | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Black/white endpoint test | Independent finish control plus Fade preset |
| Gaussian Blur | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Impulse + alpha tests | Radius capped at 6 px for bounded local rendering |
| Focus Blur | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Center-protection test | Fixed centered elliptical focus region |
| Radial Blur | WORKING | `editor/EditorSpatialEngine.kt` | Yes | Yes | Yes | Yes | Center/outer-region test | Fixed image-center origin; 8 px maximum sample distance |
| Bokeh/Portrait Blur | NOT IMPLEMENTED | — | No | No | No | — | No | Needs subject/portrait mask |
| Black & White | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog tests | Preset-based |
| Film looks | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog + screenshot tests | Film, Vintage, Fade, Cinema, and Cinematic looks |
| Cinematic presets | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog + screenshot tests | Distinct Cinema and Cinematic looks |
| Crop | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Geometry + screenshot tests | Centered aspect crop after optional free crop |
| Free Crop | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Four-edge crop test | Four independent normalized trim sliders; no direct canvas handles |
| Rotate | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Geometry tests | 90-degree increments; fine rotation is provided by Straighten |
| Flip Horizontal | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Geometry + session tests | — |
| Flip Vertical | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Geometry + session tests | — |
| Straighten | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Fill/dimension test | ±15° with automatic edge-filling scale |
| Perspective Horizontal | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Warp test | Manual signed correction; no automatic line detection |
| Perspective Vertical | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Warp test | Manual signed correction; no automatic line detection |
| Aspect Ratio | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Preset dimension tests | Centered within the active free-crop bounds |
| Custom Aspect Ratio | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Custom ratio test | Width/height range 1–20 |
| Grid overlay | WORKING | `AlphaPicsEditorScreen.kt` | Yes | N/A | Yes | Yes | Roborazzi screenshot | Crop-only overlay; intentionally excluded from export |
| Rule of thirds | WORKING | `AlphaPicsEditorScreen.kt` | Yes | N/A | Yes | Yes | Active-grid screenshot | Crop-only overlay; intentionally excluded from export |
| Social aspect presets | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Preset dimension tests | Includes 1:1, 4:5, 3:4, 2:3, 3:2, 4:3, 16:9, 9:16, and 1.91:1 |
| Barrel/Pincushion correction | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Real warp + progress tests | One signed manual distortion control; no EXIF lens profile |
| Horizontal geometry | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Real warp test | Manual axial stretch |
| Vertical geometry | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Real warp test | Manual axial stretch |
| Manual lens-style correction | WORKING | `editor/EditorGeometryEngine.kt` | Yes | Yes | Yes | Yes | Warp + cancellation tests | Manual distortion and axial controls; no automatic camera-profile lookup |
| Healing | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Locality + progress tests | Deterministic neighboring-source blend; not semantic/content-aware AI |
| Clone | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Source-offset sampling test | Manual X/Y offset sliders; no draggable source pin |
| Blemish Removal | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Spatial brush tests | Painted local smoothing; no automatic face/blemish detection |
| Red-eye Correction | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Red-selectivity test | Painted red-dominance correction; no automatic eye detection |
| Local Blur | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Local spatial test | Maximum local kernel radius is 6 px per replay sample |
| Local Sharpen | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Local spatial test | Painted 3×3 unsharp result |
| Exposure brush | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Painted-region isolation test | Positive local exposure only |
| Brightness brush | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Erase/baseline test | Positive local brightness only |
| Saturation brush | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Progress replay test | Positive local saturation only |
| Temperature brush | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Alpha-preservation test | Warm local temperature only |
| Blur brush | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Local spatial test | Same engine as Local Blur |
| Sharpen brush | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Local spatial test | Same engine as Local Sharpen |
| Brush size | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Bounds/sanitization test | Diameter range 1–30% of shorter image edge |
| Brush feather | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Bounds/sanitization test | Smooth radial edge falloff |
| Brush strength | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Bounds/sanitization test | Range 1–100% |
| Erase mask | WORKING | `editor/EditorRetouchEngine.kt` | Yes | Yes | Yes | Yes | Baseline restoration test | Restores the pre-retouch image under the erase stroke |
| Show mask | WORKING | `AlphaPicsEditorScreen.kt` | Yes | N/A | Yes | Yes | Mask Roborazzi screenshot | Cyan Retouch-only overlay; intentionally excluded from export |
| Reset mask | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Independent reset test | Resets all local Retouch strokes and settings |
| Brush Undo | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Stroke-history bounds test | Removes the latest working stroke; session redo begins after Apply |
| Add/Edit text | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel/model + Roborazzi tests | Inspector edits the latest text item; 64-item bound |
| Text font size | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel + screenshot tests | 2–24% of shorter edge before scale |
| Text weight | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel + screenshot tests | Regular or bold system typeface; font-family picker not yet available |
| Text alignment | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel/model tests | Left, center, right |
| Text color | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel + screenshot tests | Curated seven-color local palette |
| Text opacity | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | 0–100% |
| Text rotation | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | ±180° |
| Text scale | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | 25–300% |
| Text position | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | Normalized X/Y sliders; no direct drag handles |
| Text letter spacing | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | Android Paint letter spacing |
| Text line spacing | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | 75–200% |
| Text background | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel test | Optional translucent dark rounded backdrop |
| Text stroke/outline | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel test | Optional black outline |
| Text shadow | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel + model test | Adjustable 0–100 strength |
| Freehand drawing | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Native compositing + screenshot tests | 128 strokes × 512 normalized points |
| Draw brush | WORKING | `AlphaPicsEditorScreen.kt` | Yes | Yes | Yes | Yes | Gesture + engine tests | Round-cap freehand brush |
| Draw eraser | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Isolated native compositing test | Clears drawing layer only; never erases source photo |
| Draw color | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Screenshot test | Curated seven-color palette |
| Draw opacity | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | 0–100% |
| Draw brush size | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | 0.2–12% of shorter edge |
| Draw Undo/Redo | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Bounded undo/redo model test | Working-stroke redo clears after a new stroke |
| Rectangle shape | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | All-shapes pixel test | — |
| Rounded rectangle shape | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | All-shapes pixel + screenshot test | Fixed proportional corner radius |
| Circle shape | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | All-shapes pixel test | Oval when width and height differ |
| Line shape | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | All-shapes pixel test | — |
| Arrow shape | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | All-shapes pixel test | Fixed proportional arrow head |
| Shape fill | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel + screenshot test | Uses selected palette color |
| Shape stroke | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel test | Round joins/caps |
| Shape stroke width | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | 0.2–8% |
| Shape opacity | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | 0–100% |
| Shape rotate | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel/model tests | ±180° |
| Shape resize | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | Independent width/height sliders |
| Shape move | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | Normalized X/Y sliders; no direct drag handles |
| Sticker/overlay add | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Sticker pixel + screenshot tests | Local Star, Heart, Sparkle catalog only |
| Sticker/overlay move | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | Normalized X/Y sliders |
| Sticker/overlay scale | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | 3–80% |
| Sticker/overlay rotate | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Native screenshot test | Path-matrix rotation |
| Sticker/overlay flip | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Sticker transform test | Horizontal flip |
| Sticker/overlay opacity | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model bounds test | 0–100% |
| Sticker/overlay delete | WORKING | `AlphaPicsEditorScreen.kt` | Yes | Yes | Yes | Yes | Screenshot fixture coverage | Deletes latest sticker |
| Sticker/overlay duplicate | WORKING | `AlphaPicsEditorScreen.kt` | Yes | Yes | Yes | Yes | Model cap test | Duplicates latest with offset |
| Border | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel + screenshot tests | Source-resolution border |
| Rounded border | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Native Roborazzi test | Rounded preset |
| White border | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Frame screenshot test | — |
| Black border | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Frame engine test | — |
| Custom border color | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Screenshot test | Curated seven-color palette |
| Custom border thickness | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Model/engine test | 0.2–12% of shorter edge |
| Rounded corners | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Alpha-corner pixel test | PNG preserves transparent corners; lossy formats flatten per encoder |
| Frame presets | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Frame screenshot test | None, White, Black, Rounded, Film |
| Text watermark | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Anchor pixel + screenshot tests | Text-only watermark |
| Image/logo watermark | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Watermark position | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Anchor pixel test | Controlled by anchor presets rather than free X/Y |
| Watermark scale | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel/model tests | 1.5–12% of shorter edge |
| Watermark rotation | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Screenshot test | ±180° |
| Watermark opacity | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Pixel test | 0–100% |
| Watermark padding | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Anchor pixel test | 0–15% UI |
| Watermark anchor presets | WORKING | `editor/EditorOverlayEngine.kt` | Yes | Yes | Yes | Yes | Anchor pixel + screenshot tests | Top-left, top-right, center, bottom-left, bottom-right |
| Before/After hold | WORKING | `AlphaPicsEditorScreen.kt` | Yes | N/A | N/A | Yes | Screenshot test | — |
| Before/After toggle | WORKING | `AlphaPicsEditorScreen.kt` | Yes | N/A | N/A | Yes | Screenshot test | — |
| Before/After draggable split | PARTIAL | `enhance/AlphaPicsEnhancementWorkspace.kt` | Enhance only | N/A | N/A | Yes | Screenshot test | Not wired in Photo Editor |
| Undo/Redo — sliders | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session tests | History checkpoint is Apply-based |
| Undo/Redo — crop | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session tests | Center aspect crop only |
| Undo/Redo — transform | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session tests | Existing transforms only |
| Undo/Redo — curves | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session tests | Apply-based checkpoint |
| Undo/Redo — filters | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session tests | Apply-based checkpoint |
| Undo/Redo — retouch | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session + stroke-history tests | Apply-based session redo; local stroke undo before Apply |
| Undo/Redo — text | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session + overlay tests | Apply-based checkpoint |
| Undo/Redo — draw | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Explicit drawing undo/redo test | Per-stroke working undo/redo plus Apply-based session history |
| Undo/Redo — overlays | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session + overlay model tests | Apply-based checkpoint |
| Undo/Redo — masks | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Retouch reset/history tests | Retouch mask state is part of Apply-based history |
| Show operation history | WORKING | `editor/EditorSession.kt`, `AlphaPicsEditorScreen.kt` | Yes | N/A | Yes | Yes | Session + Roborazzi tests | Named session-only checkpoints; maximum 64 including Original |
| Jump back | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Jump/branch session + UI tests | Rebuilds linear undo/redo stacks; a new edit replaces the abandoned future branch |
| Undo step | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session + Roborazzi tests | Available in the top bar and Edit History panel |
| Redo step | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session + Roborazzi tests | Available in the top bar and Edit History panel |
| Reset current control | PARTIAL | `AlphaPicsEditorScreen.kt` | Category only | Yes | Yes | Yes | Session tests | No per-control reset gesture |
| Reset category | WORKING | `editor/EditorSession.kt` | Yes | Yes | Yes | Yes | Session tests | Existing categories only |
| Reset all | WORKING | `editor/EditorSession.kt`, `AlphaPicsEditorScreen.kt` | Yes | Yes | Yes | Yes | Session + Roborazzi tests | Explicit action in Edit History; creates a reversible named checkpoint |
| Luminance histogram | WORKING | `editor/EditorHistogramEngine.kt` | Yes | N/A | N/A | Yes | Exact-bin, sampling + Roborazzi tests | Preview analysis is bounded to 262,144 samples and ignores transparent pixels |
| RGB histogram | WORKING | `editor/EditorHistogramEngine.kt` | Yes | N/A | N/A | Yes | Exact-bin, bounded-sampling + Roborazzi tests | Independent normalized red, green, and blue curves |
| Portrait preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog + Roborazzi tests | Deterministic local portrait-oriented grade; no face detection |
| Natural preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog + Roborazzi tests | Deterministic local grade |
| Cinematic preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog tests | — |
| Film preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog + Roborazzi tests | Deterministic local grade |
| Vintage preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog tests | — |
| Street preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog + Roborazzi tests | Deterministic local grade |
| Food preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog + Roborazzi tests | Deterministic local grade |
| Travel preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog + Roborazzi tests | Deterministic local grade |
| Landscape preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog + Roborazzi tests | Deterministic local grade |
| Black & White preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog tests | Named Mono |
| Warm preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog tests | — |
| Cool preset | WORKING | `editor/EditorModel.kt` | Yes | Yes | Yes | Yes | Catalog tests | — |
| Preset intensity | WORKING | `editor/EditorModel.kt`, `EditorPresetStore.kt` | Yes | Yes | Yes | Yes | Store/model + Roborazzi tests | Built-in and whole custom-look intensity are visible and adjustable from 0–100% |
| Preset favorites | WORKING | `editor/EditorPresetStore.kt` | Yes | N/A | N/A | Yes | Persistence + Roborazzi tests | Stored locally in dedicated SharedPreferences; favorites sort first |
| Custom preset | WORKING | `editor/EditorPresetStore.kt` | Yes | Yes | Yes | Yes | Persistence/intensity tests | Captures Light, Color, and Filter only; other editor categories remain unchanged; maximum 20 |
| Save preset | WORKING | `editor/EditorPresetStore.kt`, `AlphaPicsEditorScreen.kt` | Yes | N/A | N/A | Yes | Persistence + save-form Roborazzi tests | Name is trimmed to 32 characters; blank names become “My look” |
| Delete custom preset | WORKING | `editor/EditorPresetStore.kt`, `AlphaPicsEditorScreen.kt` | Yes | N/A | N/A | Yes | Persistence tests | Deletes the reusable preset but preserves the currently rendered working look |
| Collage 2-photo | WORKING | `collage/CollageModel.kt`, `CollageEngine.kt` | Yes | Yes | Yes | Yes | Model + engine + screenshots | Split and stacked layouts |
| Collage 3-photo | WORKING | `collage/CollageModel.kt`, `CollageEngine.kt` | Yes | Yes | Yes | Yes | Model + engine + screenshots | Feature and row layouts |
| Collage 4-photo | WORKING | `collage/CollageModel.kt`, `CollageEngine.kt` | Yes | Yes | Yes | Yes | Model + engine + screenshots | 2×2 grid |
| Collage more grids | WORKING | `collage/CollageModel.kt`, `CollageEngine.kt` | Yes | Yes | Yes | Yes | Six-photo engine test + screenshots | Five-photo mosaic and six-photo grid; maximum 6 |
| Collage freestyle | WORKING | `collage/CollageModel.kt`, `AlphaPicsCollageScreen.kt` | Yes | Yes | Yes | Yes | Bounds + render + screenshots | Overlapping normalized frames; maximum 6 |
| Collage drag/reposition | WORKING | `ui/alphapics/collage/AlphaPicsCollageScreen.kt` | Live | Yes | Yes | Yes | Bounds + screenshot tests | Toggle between frame drag and photo pan in Freestyle |
| Collage zoom | WORKING | `collage/CollageModel.kt`, `AlphaPicsCollageScreen.kt` | Live | Yes | Yes | Yes | Transform-bound test | Per-photo pinch zoom, clamped to 1×–4× |
| Collage swap | WORKING | `ui/alphapics/collage/AlphaPicsCollageScreen.kt` | Live | Yes | Yes | Yes | Undo/redo UI + screenshots | Swaps selected photo with the next slot |
| Collage spacing | WORKING | `collage/CollageEngine.kt` | Live | Yes | Yes | Yes | Engine + screenshots | 0–12% of the shorter output edge |
| Collage corner radius | WORKING | `collage/CollageEngine.kt` | Live | Yes | Yes | Yes | Engine + screenshots | Slot-relative radius |
| Collage background color | WORKING | `collage/CollageEngine.kt`, `AlphaPicsCollageScreen.kt` | Live | Yes | Yes | Yes | Engine + screenshots | Curated seven-color palette |
| Collage gradient background | WORKING | `collage/CollageEngine.kt`, `AlphaPicsCollageScreen.kt` | Live | Yes | Yes | Yes | Engine + screenshots | Deterministic diagonal two-color gradient |
| Collage image background | WORKING | `collage/CollageExportManager.kt`, `AlphaPicsCollageScreen.kt` | Live | Yes | Yes | Yes | Export guards + screenshots | Requires a device image; cancellation is not silently exported |
| Collage border | WORKING | `collage/CollageEngine.kt` | Live | Yes | Yes | Yes | Pixel render + screenshots | Shared color; 0–6% width |
| Collage aspect ratio | WORKING | `collage/CollageModel.kt`, `AlphaPicsCollageScreen.kt` | Live | Yes | Yes | Yes | Dimension + undo/redo + screenshots | 1:1, 4:5, 3:4, 9:16, 16:9, and 4:3 |
| Collage text | WORKING | `editor/EditorOverlayEngine.kt`, `AlphaPicsCollageScreen.kt` | Live | Yes | Yes | Yes | Overlay engine + screenshots | Controls edit the most recently added text item |
| Collage stickers | WORKING | `editor/EditorOverlayEngine.kt`, `AlphaPicsCollageScreen.kt` | Live | Yes | Yes | Yes | Overlay engine + screenshots | Three original built-in vector kinds; latest-item controls |
| Collage high-resolution export | WORKING | `collage/CollageExportManager.kt` | Yes | Yes | Yes | Yes | 1024px MediaStore readback + export guards | JPEG/PNG/WebP at 2K, 3K, or 4K long edge; explicit memory failure |
| Resize width | WORKING | `photo/PhotoUtilityModel.kt`, `photo/PhotoUtilityEngine.kt` | Live | Yes | Yes | Yes | Model + export + screenshots | 1–8192 px; output also limited to 64 MP |
| Resize height | WORKING | `photo/PhotoUtilityModel.kt`, `photo/PhotoUtilityEngine.kt` | Live | Yes | Yes | Yes | Model + export + screenshots | 1–8192 px; output also limited to 64 MP |
| Resize percentage | WORKING | `photo/PhotoUtilityModel.kt`, `AlphaPicsPhotoUtilityScreen.kt` | Live | Yes | Yes | Yes | Percentage math + Undo/Redo + screenshot | 1–400% |
| Resize maintain aspect ratio | WORKING | `photo/PhotoUtilityModel.kt`, `AlphaPicsPhotoUtilityScreen.kt` | Live | Yes | Yes | Yes | Preset/aspect + screenshot tests | Lock updates the paired dimension |
| Resize high-quality resampling | WORKING | `photo/PhotoResampler.kt` | Live | Yes | Yes | Yes | Exact size/progress + alpha tests | Multi-pass filtered scaling; explicit memory failure |
| Resize dimension presets | WORKING | `photo/PhotoUtilityModel.kt`, `AlphaPicsPhotoUtilityScreen.kt` | Live | Yes | Yes | Yes | Landscape/portrait preset + screenshot tests | Original plus 1080/2048/4096 long edge |
| Batch Resize same size | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Resize same percentage | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Resize maintain aspect ratio | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Resize progress | NOT IMPLEMENTED | — | No | No | No | — | No | Existing progress is compressor-only |
| Batch Resize cancel | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Resize per-item errors | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| JPEG conversion | WORKING | `photo/PhotoUtilityEngine.kt` | N/A | Yes | Yes | Yes | EXIF/readability integration tests | Transparency is flattened on white |
| PNG conversion | WORKING | `photo/PhotoUtilityEngine.kt` | N/A | Yes | Yes | Yes | Exact-dimension MediaStore readback | Quality control is correctly omitted; alpha retained |
| WebP conversion | WORKING | `photo/PhotoUtilityEngine.kt` | N/A | Yes | Yes | Yes | Exact-dimension MediaStore readback | Lossy quality on supported Android encoder |
| AVIF conversion | UNSUPPORTED | — | No | No | No | — | No | Android Bitmap encoder has no reliable AVIF output API across minSdk 24–36 |
| Metadata viewer | WORKING | `photo/PhotoMetadataReader.kt`, `AlphaPicsPhotoUtilityScreen.kt` | Viewer | N/A | N/A | Yes | Reader + screenshot tests | Device URI photos only |
| EXIF viewer | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | Orientation/common-field tests | Curated common safe fields; not every vendor tag |
| Metadata dimensions | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | Raw/oriented dimension tests | Display dimensions account for EXIF rotation |
| Metadata file size | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | Real file metadata test | Provider may report unknown size |
| Metadata format | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | PNG/JPEG tests | Derived from decoder MIME with URI fallback |
| Metadata orientation | WORKING | `photo/PhotoMetadataReader.kt`, `photo/PhotoUtilityEngine.kt` | Viewer | Normalized | N/A | Yes | Rotated EXIF + export tests | Output orientation normalized |
| Metadata date | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | Safe JPEG preserve | N/A | Yes | EXIF original-date test | Shown only when source/provider supplies a date |
| Remove metadata | WORKING | `photo/PhotoUtilityEngine.kt` | Policy | Yes | Yes | Yes | JPEG removal + PNG integration tests | Default policy; encoded output retains required format headers only |
| Preserve metadata | WORKING | `photo/PhotoUtilityEngine.kt` | Policy | Yes | Yes | Yes | Safe JPEG preserve/rejection tests | JPEG only; curated camera/date fields, GPS always omitted |
| Image Info dimensions | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | Reader + screenshot tests | — |
| Image Info megapixels | WORKING | `photo/PhotoUtilityModel.kt` | Viewer | N/A | N/A | Yes | Dimension-derived model coverage | Rounded to two decimals in UI |
| Image Info aspect ratio | WORKING | `photo/PhotoUtilityModel.kt` | Viewer | N/A | N/A | Yes | Dimension-derived model coverage | Decimal ratio display |
| Image Info format | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | PNG/JPEG reader tests | — |
| Image Info file size | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | Real file reader test | Provider may report unknown size |
| Image Info transparency | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | Transparent PNG reader test | Bounded 512px sample can miss isolated alpha in very large images |
| Image Info EXIF orientation | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | 90° orientation test | — |
| Image Info color information | WORKING | `photo/PhotoMetadataReader.kt` | Viewer | N/A | N/A | Yes | Reader test | Reports decoded color space/gamut/config, not embedded ICC bytes |
| Skin smoothing | NOT IMPLEMENTED | — | No | No | No | — | No | Needs portrait-local mask |
| Blemish correction | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Face brightness | NOT IMPLEMENTED | — | No | No | No | — | No | Needs face region/landmarks |
| Eye brightness | NOT IMPLEMENTED | — | No | No | No | — | No | Needs eye landmarks |
| Teeth brightness | NOT IMPLEMENTED | — | No | No | No | — | No | Needs mouth landmarks |
| Face tone | NOT IMPLEMENTED | — | No | No | No | — | No | Needs face mask |
| Portrait blur | NOT IMPLEMENTED | — | No | No | No | — | No | Needs person segmentation |
| Face Detection | NOT IMPLEMENTED | — | No | N/A | No | — | No | ML stack not selected |
| Person Segmentation | NOT IMPLEMENTED | — | No | No | No | — | No | ML stack not selected |
| Subject Segmentation | NOT IMPLEMENTED | — | No | No | No | — | No | ML stack not selected |
| Background Mask | NOT IMPLEMENTED | — | No | No | No | — | No | ML stack not selected |
| Foreground Cutout | NOT IMPLEMENTED | — | No | No | No | — | No | ML stack not selected |
| Face Landmarks | NOT IMPLEMENTED | — | No | N/A | No | — | No | ML stack not selected |
| Subject Selection | NOT IMPLEMENTED | — | No | No | No | — | No | ML stack not selected |
| Background Selection | NOT IMPLEMENTED | — | No | No | No | — | No | ML stack not selected |
| Offline background removal | NOT IMPLEMENTED | — | No | No | No | — | No | Awaiting single ML stack evaluation |
| Transparent background output | NOT IMPLEMENTED | — | No | No | No | — | No | Awaiting mask engine |
| Solid replacement background | NOT IMPLEMENTED | — | No | No | No | — | No | Awaiting mask engine |
| Custom replacement background | NOT IMPLEMENTED | — | No | No | No | — | No | Awaiting mask engine |
| Background edge refinement | NOT IMPLEMENTED | — | No | No | No | — | No | Awaiting mask engine |
| Brush Mask | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Radial Mask | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Linear Gradient Mask | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Subject Mask | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Background Mask selection | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Masked Exposure | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Masked Contrast | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Masked Saturation | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Masked Temperature | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Masked Blur | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Masked Sharpen | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio crop | PARTIAL | Editor crop | Yes | Yes | Yes | Yes | Session tests | No dedicated workflow |
| Product Studio resize | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio canvas | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio padding | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio alignment | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio solid background | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio custom background | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio logo | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio watermark | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio text | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio marketplace ratios | PARTIAL | Editor aspect presets | Yes | Yes | Yes | Yes | Session tests | No marketplace labels/workflow |
| Product Studio batch formatting | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Product Studio background removal | NOT IMPLEMENTED | — | No | No | No | — | No | Awaiting segmentation |
| Batch Compress | WORKING | `batch/BatchImageProcessor.kt` | Status | Yes | N/A | Yes | Batch tests | Protected existing workflow; max 20 |
| Batch Resize | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Convert | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Watermark | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Logo | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Padding | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Alignment | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch Preset | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Batch per-item status | WORKING | `batch/BatchModels.kt` | Yes | Yes | N/A | Yes | Batch tests | Compression workflow only |
| Batch per-item fault isolation | WORKING | `batch/BatchImageProcessor.kt` | Yes | Yes | N/A | Yes | Batch tests | Compression workflow only |
| Social export 1:1 | WORKING | `editor/EditorExportManager.kt` | Yes | Yes | Yes | Yes | Session tests | Center crop |
| Social export 4:5 | WORKING | `editor/EditorExportManager.kt` | Yes | Yes | Yes | Yes | Session tests | Center crop |
| Social export 9:16 | WORKING | `editor/EditorExportManager.kt` | Yes | Yes | Yes | Yes | Session tests | Center crop |
| Social export 16:9 | WORKING | `editor/EditorExportManager.kt` | Yes | Yes | Yes | Yes | Session tests | Center crop |
| Social export 3:4 | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Social export 4:3 | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Advanced export JPEG | PARTIAL | `editor/EditorExportManager.kt` | N/A | Yes | N/A | Yes | No integration test | Export result has a recycled-bitmap dimension bug |
| Advanced export PNG | PARTIAL | `editor/EditorExportManager.kt` | N/A | Yes | N/A | Yes | No integration test | Export result has a recycled-bitmap dimension bug |
| Advanced export WebP | PARTIAL | `editor/EditorExportManager.kt` | N/A | Yes | N/A | Yes | No integration test | Export result has a recycled-bitmap dimension bug |
| Advanced export quality | WORKING | `AlphaPicsEditorScreen.kt` | N/A | Yes | N/A | Yes | Screenshot test | JPEG/WebP only |
| Advanced export resolution | NOT IMPLEMENTED | — | No | No | No | — | No | Hard safety cap currently downsamples originals |
| Advanced export metadata choice | NOT IMPLEMENTED | — | No | No | No | — | No | — |
| Advanced export file name | PARTIAL | `editor/EditorExportManager.kt` | Generated | Yes | N/A | Yes | No | Not user-editable |
| Advanced export destination | PARTIAL | `editor/EditorExportManager.kt` | Fixed | Yes | N/A | Yes | No | Fixed Pictures/AlphaPics AI destination |
| Advanced export final dimensions | WORKING | `editor/EditorExportManager.kt` | After export | Yes | N/A | Yes | Bitmap renderer tests | Captured before bitmap recycling |
| Advanced export actual encoded size | WORKING | `editor/EditorExportManager.kt` | After export | Yes | N/A | Yes | No | — |
| Advanced export Save | PARTIAL | `AlphaPicsEditorScreen.kt` | N/A | Yes | N/A | Yes | Screenshot test | No export integration test |
| Advanced export Share | PARTIAL | `AlphaPicsEditorScreen.kt` | N/A | Yes | N/A | Yes | Screenshot test | Depends on successful export |
| Advanced export Edit Again | PARTIAL | `AlphaPicsEditorScreen.kt` | Session remains | N/A | Yes | Yes | Session test | Label/action is Done rather than Edit Again |
| Advanced export Compare | WORKING | `AlphaPicsEditorScreen.kt` | Yes | N/A | N/A | Yes | Screenshot test | Available before export, not result sheet |
| Video Trim | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Split | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Crop | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Rotate | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Aspect Ratio | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Speed | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Mute | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Audio Extraction | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Music Overlay | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Text | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Stickers | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Basic Transitions | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Filters | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Color Adjustments | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video Frame Export | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |
| Video GIF Creation | NOT IMPLEMENTED | — | No | No | No | — | No | Deferred until photo studio is green |

## Audit notes

- The Home screen and completed Home banner-removal result are outside this matrix and must not be edited.
- Existing compression, batch compression, history, ads/tokens, camera/gallery, MediaStore save/share,
  Room, application ID, and signing are protected.
- Cloud-only AI Restore, AI Face Enhance, cloud super-resolution, generative fill/remove,
  text-to-image, image-to-image, and AI video generation remain intentionally unclaimed.
- Light, Color, HSL, Curves, Color Mix, Split Toning, Color Grading, Detail, and Effects share
  the same deterministic preview/export pipeline.
- Text, Draw, Shapes, local Stickers, Frames, and text Watermarks now share the same
  deterministic preview/full-resolution export path. Image/logo watermarking and font-family
  selection remain honestly unavailable.
- Histogram, the expanded preset catalog, favorites, bounded custom looks, visible intensity,
  and named session history are now locally implemented and verified. Custom presets intentionally
  capture Light, Color, and Filter only; HSL, curves, geometry, retouch, and overlays are preserved.
- Collage Studio now uses one deterministic compositor for its bounded live preview and original-
  source export, with 2–6 photo grids/Freestyle, per-photo transform, document undo/redo,
  local overlays, EXIF normalization, MediaStore save, and honest 2K/3K/4K controls.
- Resize, conversion, and source Image Info now use original device URIs, bounded preview work,
  exact-dimension MediaStore readback tests, explicit metadata policies, and a shared photo-first
  native workspace. AVIF remains unsupported for the stated platform-encoder reason.
- The next implementation gate is Batch Studio.
