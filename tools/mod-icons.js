// Builds src/main/resources/assets/valor/textures/mod-icons.png from mod-icons.txt next to it: each listed Lucide icon,
// white on transparent, in an 8 x 8 grid of 64 px cells. Usage: node tools/mod-icons.js <path to the lucide-static package>
// (npm install lucide-static). Needs Playwright with Chromium.
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const dir = path.join(__dirname, '..', 'src', 'main', 'resources', 'assets', 'valor', 'textures');
const lucide = process.argv[2];
if (!lucide) throw new Error('Usage: node tools/mod-icons.js <path to lucide-static>');
const names = fs.readFileSync(path.join(dir, 'mod-icons.txt'), 'utf8').split('\n')
  .map((l) => l.trim()).filter((l) => l && !l.startsWith('#')).map((l) => l.split(/\s+/)[1]);
if (names.length > 64) throw new Error('mod-icons.png holds 64 icons');

const cells = names.map((n, i) => {
  const svg = fs.readFileSync(path.join(lucide, 'icons', `${n}.svg`), 'utf8')
    .replace(/width="24"/, 'width="52"').replace(/height="24"/, 'height="52"').replace(/currentColor/g, '#ffffff');
  return `<div style="position:absolute;left:${(i % 8) * 64 + 6}px;top:${Math.floor(i / 8) * 64 + 6}px">${svg}</div>`;
}).join('');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 512, height: 512 } });
  await page.setContent(`<body style="margin:0;background:transparent">${cells}</body>`);
  await page.screenshot({ path: path.join(dir, 'mod-icons.png'), omitBackground: true });
  await browser.close();
})();
