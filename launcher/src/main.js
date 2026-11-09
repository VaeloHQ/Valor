// Valor launcher main process: the window, and everything the page asks for over IPC.
const { app, BrowserWindow, ipcMain, shell } = require('electron');
const fs = require('fs');
const path = require('path');
const game = require('./game');
const auth = require('./auth');
const servers = require('./servers');
const store = require('./store');

const NEWS_URL = 'https://raw.githubusercontent.com/VaeloHQ/Valor/master/launcher/news.json';
let win;
let running = null;

function createWindow() {
  win = new BrowserWindow({
    width: 1180, height: 720, minWidth: 980, minHeight: 620, frame: false, backgroundColor: '#05070d', show: false,
    icon: path.join(__dirname, 'ui', 'img', 'logo.png'),
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, nodeIntegration: false, sandbox: true },
  });
  win.once('ready-to-show', () => win.show());
  win.loadFile(path.join(__dirname, 'ui', 'index.html'));
  // Links in news open in the browser, never inside the launcher.
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('https://')) shell.openExternal(url);
    return { action: 'deny' };
  });
  win.webContents.on('will-navigate', (e) => e.preventDefault());
}

/** Settings as the page sees them: no tokens. */
function publicSettings() {
  const s = store.get();
  return { ...s, accounts: s.accounts.map(({ type, name, uuid }) => ({ type, name, uuid })), gameDir: game.GAME, running: !!running,
    totalMemory: Math.floor(require('os').totalmem() / 1048576) };
}

/** The Valor mod to install: bundled with the packaged launcher, or the newest local build when developing. */
function modJar() {
  if (app.isPackaged) return path.join(process.resourcesPath, 'valor.jar');
  const libs = path.join(__dirname, '..', '..', 'build', 'libs');
  const jars = fs.existsSync(libs) ? fs.readdirSync(libs).filter((f) => /^valor-.*\.jar$/.test(f)) : [];
  return jars.length ? path.join(libs, jars.sort().pop()) : null;
}

const EDITABLE = ['ram', 'width', 'height', 'fullscreen', 'javaPath', 'jvmArgs', 'afterLaunch', 'clientId', 'servers'];

ipcMain.handle('settings:get', () => publicSettings());
ipcMain.handle('settings:set', (e, patch) => {
  const clean = {};
  for (const k of EDITABLE) if (k in patch) clean[k] = patch[k];
  store.set(clean);
  return publicSettings();
});

ipcMain.handle('accounts:addOffline', (e, name) => {
  if (!/^[A-Za-z0-9_]{3,16}$/.test(name)) throw new Error('Use 3 to 16 letters, numbers or _.');
  const s = store.get();
  const accounts = s.accounts.filter((a) => !(a.type === 'offline' && a.name.toLowerCase() === name.toLowerCase()));
  accounts.push({ type: 'offline', name });
  store.set({ accounts, active: accounts.length - 1 });
  return publicSettings();
});

ipcMain.handle('accounts:addMicrosoft', async () => {
  const s = store.get();
  if (!s.clientId) throw new Error('Microsoft sign-in needs an Azure client ID. Add it in Settings.');
  const { account, refresh } = await auth.signIn(win, s.clientId);
  const accounts = s.accounts.filter((a) => !(a.type === 'msa' && a.uuid === account.uuid));
  accounts.push({ type: 'msa', name: account.name, uuid: account.uuid, refresh: store.seal(refresh) });
  store.set({ accounts, active: accounts.length - 1 });
  return publicSettings();
});

ipcMain.handle('accounts:select', (e, i) => {
  if (i >= 0 && i < store.get().accounts.length) store.set({ active: i });
  return publicSettings();
});

ipcMain.handle('accounts:remove', (e, i) => {
  const accounts = store.get().accounts.slice();
  accounts.splice(i, 1);
  store.set({ accounts, active: Math.max(0, Math.min(store.get().active, accounts.length - 1)) });
  return publicSettings();
});

/** The account to launch with; Microsoft accounts get a fresh Minecraft token from their stored refresh token. */
async function launchAccount() {
  const s = store.get();
  const a = s.accounts[s.active];
  if (!a) throw new Error('Add an account first.');
  if (a.type === 'offline') return { type: 'offline', name: a.name };
  const refresh = store.unseal(a.refresh);
  if (!refresh || !s.clientId) throw new Error('Sign in with Microsoft again.');
  const r = await auth.refresh(s.clientId, refresh);
  a.refresh = store.seal(r.refresh);
  a.name = r.account.name;
  store.save();
  return r.account;
}

ipcMain.handle('launch', async (e, server) => {
  if (running) throw new Error('Valor is already running.');
  const account = await launchAccount();
  let last = 0;
  const progress = (label, done, total) => {
    const now = Date.now();
    if (now - last < 50 && done < total) return;   // keep IPC traffic light during thousands of downloads
    last = now;
    win.webContents.send('progress', { label, done, total });
  };
  const s = store.get();
  running = await game.launch(account, { ...s, server }, modJar(), progress);
  win.webContents.send('progress', null);
  running.on('exit', (code) => {
    running = null;
    if (!win.isDestroyed()) {
      if (win.isMinimized()) win.restore();
      win.webContents.send('game-exit', code);
    }
  });
  if (s.afterLaunch === 'close') app.quit();
  else if (s.afterLaunch === 'minimize') win.minimize();
  return true;
});

ipcMain.handle('servers:ping', (e, host, port) => servers.ping(host, port));

ipcMain.handle('news', async () => {
  try {
    const res = await fetch(NEWS_URL, { headers: { 'User-Agent': 'ValorLauncher' } });
    if (res.ok) return await res.json();
  } catch { /* offline: use the copy shipped with the launcher */ }
  return JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'news.json'), 'utf8'));
});

ipcMain.handle('openGameFolder', () => shell.openPath(game.GAME));
ipcMain.on('window', (e, action) => {
  if (action === 'minimize') win.minimize();
  else if (action === 'maximize') (win.isMaximized() ? win.unmaximize() : win.maximize());
  else if (action === 'close') win.close();
});

app.whenReady().then(createWindow);
app.on('window-all-closed', () => app.quit());
