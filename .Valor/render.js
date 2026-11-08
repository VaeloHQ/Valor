const { chromium } = require('playwright');
const fs = require('fs');
(async () => {
  const [svg, out, size, bg] = process.argv.slice(2);
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: +size, height: +size } });
  const src = fs.readFileSync(svg, 'utf8');
  await p.setContent(`<body style="margin:0;background:${bg||'transparent'}">${src.replace(/<svg /, `<svg style="width:${size}px;height:${size}px;display:block" `)}</body>`);
  await p.screenshot({ path: out, omitBackground: !bg });
  await b.close();
})();
