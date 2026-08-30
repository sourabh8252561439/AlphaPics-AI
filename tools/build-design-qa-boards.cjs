const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const root = path.resolve(__dirname, '..');
const screenshotDir = path.join(root, 'app', 'src', 'test', 'screenshots');
const outputDir = path.join(root, 'design-qa');
fs.mkdirSync(outputDir, { recursive: true });

const colors = {
  background: '#050812',
  panel: '#111A2E',
  border: '#223555',
  white: '#F7F9FF',
  cyan: '#52E2FF',
  secondary: '#A9B4D1',
  blue: '#3D7DFF',
};

function escapeXml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}

function wrap(text, maxChars) {
  const words = text.split(/\s+/);
  const lines = [];
  let line = '';
  for (const word of words) {
    const candidate = line ? `${line} ${word}` : word;
    if (candidate.length > maxChars && line) {
      lines.push(line);
      line = word;
    } else {
      line = candidate;
    }
  }
  if (line) lines.push(line);
  return lines;
}

function truthPanelSvg(title, principles, width, height) {
  let y = 76;
  const rows = [];
  rows.push(`<text x="54" y="${y}" fill="${colors.cyan}" font-family="sans-serif" font-size="22" font-weight="700" letter-spacing="3">DESIGN TRUTH</text>`);
  y += 56;
  for (const line of wrap(title, 28)) {
    rows.push(`<text x="54" y="${y}" fill="${colors.white}" font-family="sans-serif" font-size="38" font-weight="750">${escapeXml(line)}</text>`);
    y += 46;
  }
  y += 32;
  principles.forEach((principle, index) => {
    const lines = wrap(principle, 38);
    rows.push(`<circle cx="64" cy="${y - 7}" r="7" fill="${index === 0 ? colors.blue : colors.cyan}"/>`);
    lines.forEach((line, lineIndex) => {
      rows.push(`<text x="88" y="${y + lineIndex * 34}" fill="${colors.secondary}" font-family="sans-serif" font-size="25" font-weight="500">${escapeXml(line)}</text>`);
    });
    y += lines.length * 34 + 30;
  });
  rows.push(`<text x="54" y="${height - 54}" fill="${colors.secondary}" font-family="sans-serif" font-size="18">Source: ALPHAPICS_UI_UX_SPEC.md</text>`);
  return Buffer.from(`
    <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
      <rect width="${width}" height="${height}" rx="34" fill="${colors.panel}" stroke="${colors.border}" stroke-width="3"/>
      ${rows.join('\n')}
    </svg>
  `);
}

async function makeBoard({ name, title, principles, screenshots }) {
  const panelWidth = 720;
  const shotWidth = screenshots.length >= 3 ? 520 : 680;
  const shotHeight = Math.round(shotWidth * 2399 / 1078);
  const gap = 36;
  const padding = 40;
  const labelHeight = 62;
  const boardHeight = Math.max(shotHeight + labelHeight + padding * 2, 1560);
  const boardWidth = padding * 2 + panelWidth + gap + screenshots.length * shotWidth + (screenshots.length - 1) * gap;

  const composites = [
    {
      input: truthPanelSvg(title, principles, panelWidth, boardHeight - padding * 2),
      left: padding,
      top: padding,
    },
  ];

  for (let index = 0; index < screenshots.length; index += 1) {
    const screenshot = screenshots[index];
    const left = padding + panelWidth + gap + index * (shotWidth + gap);
    const image = await sharp(path.join(screenshotDir, screenshot.file))
      .resize({ width: shotWidth, height: shotHeight, fit: 'contain', background: colors.background })
      .png()
      .toBuffer();
    composites.push({ input: image, left, top: padding + labelHeight });
    composites.push({
      input: Buffer.from(`
        <svg width="${shotWidth}" height="${labelHeight}" xmlns="http://www.w3.org/2000/svg">
          <text x="0" y="40" fill="${colors.white}" font-family="sans-serif" font-size="25" font-weight="700">${escapeXml(screenshot.label)}</text>
        </svg>
      `),
      left,
      top: padding,
    });
  }

  await sharp({
    create: {
      width: boardWidth,
      height: boardHeight,
      channels: 4,
      background: colors.background,
    },
  })
    .composite(composites)
    .png()
    .toFile(path.join(outputDir, name));
}

async function main() {
  await makeBoard({
    name: 'comparison-home.png',
    title: 'Focused, photo-first Home',
    principles: [
      'Brand and Settings lead into one dominant AI Photo Enhancer hero.',
      'Gallery and Camera are obvious, immediate actions.',
      'Enhance, Restore, Upscale, and Remove Background share one compact shortcut row.',
      'Edit Photo is singular and visually distinct.',
      'Compress, Batch, and History remain compact Quick Tools.',
      'No generation features and no equal-weight dashboard grid.',
    ],
    screenshots: [{ file: 'greeting.png', label: 'Implementation · Home' }],
  });

  await makeBoard({
    name: 'comparison-enhancement.png',
    title: 'Result-first enhancement workspace',
    principles: [
      'The image canvas visually dominates the screen.',
      'Before and After architecture is explicit without inventing a result.',
      'A compact bottom mode rail exposes nine enhancement intents.',
      'Unavailable processing is labelled honestly as Coming Soon.',
      'Empty entry and selected-photo states retain the same hierarchy.',
    ],
    screenshots: [
      { file: 'enhance.png', label: 'Implementation · Empty' },
      { file: 'enhancement_workspace.png', label: 'Implementation · Selected photo' },
    ],
  });

  await makeBoard({
    name: 'comparison-editor.png',
    title: 'Contextual professional editor',
    principles: [
      'The photo remains the dominant surface while controls stay close to the thumb zone.',
      'Adjust exposes four compact sliders with persistent Reset, Cancel, and Apply.',
      'Filters use real photo thumbnails and local deterministic previews.',
      'Crop groups aspect, rotate, flip, Cancel, and Apply in one contextual panel.',
      'The bottom rail is compact and horizontally navigable.',
      'Preview-only export limits are explicit and the source remains unchanged.',
    ],
    screenshots: [
      { file: 'editor_adjust.png', label: 'Implementation · Adjust' },
      { file: 'editor_filters.png', label: 'Implementation · Filters' },
      { file: 'editor_crop.png', label: 'Implementation · Crop' },
    ],
  });

  await makeBoard({
    name: 'comparison-states.png',
    title: 'Honest operational states',
    principles: [
      'Empty states provide a single clear next action.',
      'Loading explains what is happening without fake progress percentages.',
      'Errors state what failed, confirm nothing changed, and offer recovery.',
      'Coming Soon states never simulate AI output, brushes, masks, or processing.',
      'Touch targets remain at least 48 dp for primary recovery actions.',
    ],
    screenshots: [
      { file: 'state_empty.png', label: 'Implementation · Empty' },
      { file: 'state_loading.png', label: 'Implementation · Loading' },
      { file: 'state_error.png', label: 'Implementation · Error' },
    ],
  });

  await makeBoard({
    name: 'comparison-protected-workflows.png',
    title: 'Protected production workflows',
    principles: [
      'Compression retains Target Size and Quality choices and existing image engines.',
      'Batch remains a dedicated workflow with an explicit 20-photo maximum.',
      'History preserves Room-backed persistence and a clear empty state.',
      'Settings stays compact, branded, and consumer-facing.',
      'Camera, Gallery, MediaStore, Save, Share, ads, and token gating remain untouched.',
    ],
    screenshots: [
      { file: 'compressor.png', label: 'Existing · Compress' },
      { file: 'batch.png', label: 'Existing · Batch' },
      { file: 'history.png', label: 'Existing · History' },
      { file: 'settings.png', label: 'Existing · Settings' },
    ],
  });
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
