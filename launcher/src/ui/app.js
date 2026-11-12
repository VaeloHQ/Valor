// Valor launcher page. Everything that touches disk, network or the game goes through window.valor (preload.js).
const $ = (id) => document.getElementById(id);
const FEATURED = [
  { name: 'Hypixel', host: 'mc.hypixel.net', port: 25565 },
  { name: 'Minemen Club', host: 'minemen.club', port: 25565 },
  { name: 'Bedwars Practice', host: 'bedwarspractice.club', port: 25565 },
];
const COLORS = { 0: '#000000', 1: '#0000aa', 2: '#00aa00', 3: '#00aaaa', 4: '#aa0000', 5: '#aa00aa', 6: '#ffaa00', 7: '#aaaaaa',
  8: '#555555', 9: '#5555ff', a: '#55ff55', b: '#55ffff', c: '#ff5555', d: '#ff55ff', e: '#ffff55', f: '#ffffff' };
let settings;

/** Turns "Error invoking remote method 'x': Error: message" into "message". */
const message = (e) => String(e && e.message || e).replace(/^Error invoking remote method '[^']+': (Error: )?/, '');

function el(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  Object.assign(node, props);
  for (const c of children) if (c != null) node.append(c);
  return node;
}

function headUrl(account) {
  return `https://mc-heads.net/avatar/${encodeURIComponent(account.type === 'msa' ? account.uuid : account.name)}/64`;
}

function headImg(account, className) {
  const img = el('img', { className, alt: '', src: account ? headUrl(account) : 'icons/user.svg' });
  img.onerror = () => { img.src = 'icons/user.svg'; };
  return img;
}

/** Server text with § colour codes as coloured spans (text only, never HTML). */
function formatted(text) {
  const frag = document.createDocumentFragment();
  let color = null;
  for (const part of text.split(/(§[0-9a-fk-or])/i)) {
    const code = /^§([0-9a-fk-or])$/i.exec(part);
    if (code) {
      const c = code[1].toLowerCase();
      if (COLORS[c]) color = COLORS[c];
      else if (c === 'r') color = null;
      continue;
    }
    if (!part) continue;
    const span = el('span', { textContent: part });
    if (color) span.style.color = color;
    frag.append(span);
  }
  return frag;
}

// ---- Navigation and window ----

function show(page) {
  document.querySelectorAll('.nav').forEach((b) => b.classList.toggle('active', b.dataset.page === page));
  document.querySelectorAll('.page').forEach((p) => p.classList.toggle('active', p.id === `page-${page}`));
  if (page === 'servers') pingAll();
}

document.querySelectorAll('.nav').forEach((b) => b.addEventListener('click', () => show(b.dataset.page)));
document.querySelectorAll('[data-win]').forEach((b) => b.addEventListener('click', () => valor.window(b.dataset.win)));

// ---- Accounts ----

function renderAccounts() {
  const active = settings.accounts[settings.active];
  $('avatar-img').replaceWith(Object.assign(headImg(active), { id: 'avatar-img' }));
  const offline = !active && settings.dev && settings.offlineName;
  $('playing').replaceChildren(...(active ? [headImg(active), el('span', {}, 'Playing as ', el('b', { textContent: active.name }))]
    : offline ? [headImg(null), el('span', {}, 'Playing offline as ', el('b', { textContent: settings.offlineName }))]
      : [el('span', { textContent: 'Sign in with Microsoft in Settings to play' })]));
  const row = (a, i, removable) => {
    const node = el('div', { className: 'account' + (i === settings.active ? ' selected' : '') }, headImg(a, 'head'),
      el('div', { className: 'who' }, a.name, el('small', { textContent: 'Microsoft account' })));
    node.addEventListener('click', async () => {
      settings = await valor.selectAccount(i);
      renderAccounts();
      $('accounts-popover').hidden = true;
    });
    if (removable) {
      const remove = el('button', { className: 'remove-account', title: 'Remove' }, el('img', { src: 'icons/trash-2.svg', alt: '' }));
      remove.addEventListener('click', async (e) => {
        e.stopPropagation();
        settings = await valor.removeAccount(i);
        renderAccounts();
      });
      node.append(remove);
    }
    return node;
  };
  $('account-list').replaceChildren(...settings.accounts.map((a, i) => row(a, i, true)));
  $('popover-list').replaceChildren(...settings.accounts.map((a, i) => row(a, i, false)));
}

$('avatar').addEventListener('click', (e) => {
  e.stopPropagation();
  $('accounts-popover').hidden = !$('accounts-popover').hidden;
});
document.addEventListener('click', (e) => {
  if (!$('accounts-popover').contains(e.target)) $('accounts-popover').hidden = true;
});
$('popover-manage').addEventListener('click', () => {
  $('accounts-popover').hidden = true;
  show('settings');
  $('accounts-card').scrollIntoView({ behavior: 'smooth' });
});

function accountError(e) {
  $('account-error').textContent = e ? message(e) : '';
  $('account-error').hidden = !e;
}

$('microsoft').addEventListener('click', async () => {
  $('microsoft').disabled = true;
  try {
    settings = await valor.addMicrosoft();
    accountError(null);
  } catch (err) {
    accountError(err);
  }
  $('microsoft').disabled = false;
  renderAccounts();
});

// ---- Launch ----

function setRunning(running) {
  $('launch').disabled = running;
  $('launch-label').textContent = running ? 'Playing' : 'Launch';
  $('profile').disabled = running;   // the game owns its profiles while it runs
  document.querySelectorAll('.fav').forEach((b) => { b.disabled = running; });
}

// ---- Profiles (made in game; picked here before launching) ----

async function loadProfiles() {
  const p = await valor.profiles();
  $('profile-pick').hidden = !p;
  if (!p) return;
  $('profile').replaceChildren(...p.names.map((n) => el('option', { value: n, textContent: n })));
  $('profile').value = p.active;
}

$('profile').addEventListener('change', async () => {
  try {
    await valor.setProfile($('profile').value);
  } catch (e) {
    $('launch-error').textContent = message(e);
    $('launch-error').hidden = false;
    loadProfiles();
  }
});

// ---- Updates ----

valor.onUpdateReady((version) => {
  $('update-label').textContent = `Restart to update to ${version}`;
  $('update').hidden = false;
});
$('update').addEventListener('click', () => valor.installUpdate());

async function launch(server) {
  $('launch-error').hidden = true;
  setRunning(true);
  $('launch-label').textContent = 'Starting';
  show('home');
  try {
    await valor.launch(server);
  } catch (e) {
    $('progress').hidden = true;
    $('launch-error').textContent = message(e);
    $('launch-error').hidden = false;
    setRunning(false);
  }
}

$('launch').addEventListener('click', () => launch(null));
valor.onProgress((p) => {
  $('progress').hidden = !p;
  if (!p) return;
  $('progress-fill').style.width = `${p.total ? (100 * p.done) / p.total : 0}%`;
  $('progress-text').textContent = p.total > 1 ? `${p.label}  ${p.done.toLocaleString()} / ${p.total.toLocaleString()}` : p.label;
});
valor.onGameExit((code) => {
  setRunning(false);
  loadProfiles();   // the game may have switched or made profiles
  if (code) {
    $('launch-error').textContent = `Valor closed with an error (code ${code}). The log is in your Valor folder: game-output.log.`;
    $('launch-error').hidden = false;
  }
});

// ---- News ----

async function loadNews() {
  let items = [];
  try {
    items = await valor.news();
  } catch { /* no news */ }
  $('news').replaceChildren(...items.slice(0, 3).map((n) => el('article', { className: 'news-card' },
    el('span', { className: 'tag', textContent: n.tag || 'News' }), el('h4', { textContent: n.title }),
    el('p', { textContent: n.body }), el('time', { textContent: new Date(n.date).toLocaleDateString(undefined, { dateStyle: 'medium' }) }))));
}

// ---- Servers ----

function allServers() {
  return [...FEATURED.map((s) => ({ ...s, featured: true })), ...settings.servers];
}

const serverKey = (s) => `${s.host}:${s.port}`;
const isFavorite = (s) => settings.favorites.includes(serverKey(s));

async function toggleFavorite(s) {
  const key = serverKey(s);
  settings = await valor.setSettings({ favorites: isFavorite(s) ? settings.favorites.filter((k) => k !== key) : [...settings.favorites, key] });
  renderServers();
  pingAll();
  renderFavorites();
}

/** Starred servers as one-click Join buttons under Launch (Quick play), with their icon and players online. */
function renderFavorites() {
  const favs = allServers().filter(isFavorite);
  $('quickplay').hidden = !favs.length;
  $('favorites').replaceChildren(...favs.map((s) => {
    const icon = el('img', { src: 'img/server.png', alt: '' });
    const status = el('small', { textContent: s.port === 25565 ? s.host : serverKey(s) });
    const b = el('button', { className: 'fav', title: `Join ${s.name}`, disabled: settings.running }, icon, el('span', {}, el('b', { textContent: s.name }), status));
    b.addEventListener('click', () => launch({ host: s.host, port: s.port }));
    valor.ping(s.host, s.port).then((r) => {
      if (r.favicon && r.favicon.startsWith('data:image/png;base64,')) icon.src = r.favicon;
      status.textContent = `${r.online.toLocaleString()} online`;
      status.className = 'online';
    }).catch(() => { status.textContent = 'Offline'; });
    return b;
  }));
}

function renderServers() {
  $('servers').replaceChildren(...allServers().map((s, i) => {
    const icon = el('img', { className: 'icon', src: 'img/server.png', alt: '' });
    const motd = el('div', { className: 'motd', textContent: 'Pinging...' });
    const address = el('span', { textContent: s.port === 25565 ? s.host : `${s.host}:${s.port}` });
    const meta = el('div', { className: 'meta' }, address);
    const join = el('button', { className: 'primary small' }, el('img', { src: 'icons/play.svg', alt: '' }), 'Join');
    join.addEventListener('click', () => launch({ host: s.host, port: s.port }));
    const star = el('button', { className: 'star' + (isFavorite(s) ? ' on' : ''), title: isFavorite(s) ? 'Remove from Quick play' : 'Add to Quick play' },
      el('img', { src: isFavorite(s) ? 'icons/star-on.svg' : 'icons/star.svg', alt: '' }));
    star.addEventListener('click', () => toggleFavorite(s));
    const card = el('div', { className: 'server' + (s.featured ? ' featured' : '') }, icon,
      el('div', {}, el('div', { className: 'name' }, s.name, s.featured ? el('span', { className: 'badge', textContent: 'Featured' }) : null), motd, meta), join, star);
    if (!s.featured) {
      const remove = el('button', { className: 'remove', title: 'Remove' }, el('img', { src: 'icons/trash-2.svg', alt: '' }));
      remove.addEventListener('click', async () => {
        const list = settings.servers.slice();
        list.splice(i - FEATURED.length, 1);
        settings = await valor.setSettings({ servers: list, favorites: settings.favorites.filter((k) => k !== serverKey(s)) });
        renderServers();
        pingAll();
        renderFavorites();
      });
      card.append(remove);
    }
    card.dataset.key = `${s.host}:${s.port}`;
    card.ping = { icon, motd, meta, address };
    return card;
  }));
}

function pingAll() {
  for (const card of $('servers').children) {
    const [host, port] = card.dataset.key.split(':');
    const { icon, motd, meta, address } = card.ping;
    valor.ping(host, Number(port)).then((r) => {
      if (r.favicon && r.favicon.startsWith('data:image/png;base64,')) icon.src = r.favicon;
      motd.replaceChildren(formatted(r.motd.trim() || r.version));
      meta.replaceChildren(el('span', { className: 'online', textContent: `${r.online.toLocaleString()} / ${r.max.toLocaleString()} online` }),
        el('span', { textContent: `${r.latency} ms` }), address);
    }).catch(() => {
      motd.textContent = "Can't reach this server";
      meta.replaceChildren(el('span', { className: 'down', textContent: 'Offline' }), address);
    });
  }
}

$('refresh-servers').addEventListener('click', () => { renderServers(); pingAll(); });
$('add-server').addEventListener('click', () => { $('add-server-form').hidden = false; $('server-name').focus(); });
$('cancel-server').addEventListener('click', () => { $('add-server-form').hidden = true; });
$('add-server-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const [host, port] = $('server-address').value.trim().split(':');
  const list = [...settings.servers, { name: $('server-name').value.trim(), host, port: Number(port) || 25565 }];
  settings = await valor.setSettings({ servers: list });
  $('add-server-form').reset();
  $('add-server-form').hidden = true;
  renderServers();
  pingAll();
});

// ---- Settings ----

function bindSettings() {
  // Up to three quarters of the PC's memory, in 256 MB steps (at least 2 GB so the slider always has room).
  $('ram').max = Math.max(2048, Math.floor((settings.totalMemory * 0.75) / 256) * 256);
  const ramText = () => { $('ram-value').textContent = `${(Number($('ram').value) / 1024).toFixed(Number($('ram').value) % 1024 ? 2 : 0)} GB`; };
  $('ram').value = settings.ram;
  ramText();
  $('ram').addEventListener('input', ramText);
  $('ram').addEventListener('change', () => valor.setSettings({ ram: Number($('ram').value) }));
  for (const [id, key] of [['width', 'width'], ['height', 'height']]) {
    $(id).value = settings[key];
    $(id).addEventListener('change', () => valor.setSettings({ [key]: Math.max(Number($(id).min), Number($(id).value) || settings[key]) }));
  }
  $('fullscreen').checked = settings.fullscreen;
  $('fullscreen').addEventListener('change', () => valor.setSettings({ fullscreen: $('fullscreen').checked }));
  for (const [id, key] of [['java-path', 'javaPath'], ['jvm-args', 'jvmArgs']]) {
    $(id).value = settings[key];
    $(id).addEventListener('change', () => valor.setSettings({ [key]: $(id).value.trim() }));
  }
  const seg = $('after-launch');
  const mark = () => seg.querySelectorAll('button').forEach((b) => b.classList.toggle('on', b.dataset.value === settings.afterLaunch));
  seg.querySelectorAll('button').forEach((b) => b.addEventListener('click', async () => {
    settings = await valor.setSettings({ afterLaunch: b.dataset.value });
    mark();
  }));
  mark();
  $('offline-field').hidden = !settings.dev;
  $('offline-name').value = settings.offlineName;
  $('offline-name').addEventListener('change', async () => {
    settings = await valor.setSettings({ offlineName: $('offline-name').value.trim() });
    renderAccounts();
  });
  $('game-dir').textContent = settings.gameDir;
  $('open-folder').addEventListener('click', () => valor.openGameFolder());
}

(async () => {
  settings = await valor.settings();
  renderAccounts();
  bindSettings();
  renderServers();
  renderFavorites();
  loadProfiles();
  setRunning(settings.running);
  loadNews();
})();
