// Launcher settings and accounts in ~/.Valor/launcher.json. Microsoft refresh tokens are encrypted with the OS keychain
// (Electron safeStorage) and are not saved at all where that isn't available.
const fs = require('fs');
const path = require('path');
const os = require('os');
const { safeStorage } = require('electron');
const { ROOT } = require('./game');

const FILE = path.join(ROOT, 'launcher.json');
const DEFAULTS = {
  ram: Math.min(4096, Math.max(1024, Math.floor(os.totalmem() / 1048576 / 4 / 256) * 256)),
  width: 1280, height: 720, fullscreen: false, javaPath: '', jvmArgs: '',
  afterLaunch: 'minimize',   // keep, minimize or close
  accounts: [],              // { type: 'msa', name, uuid, refresh (encrypted) }
  active: 0,
  servers: [],               // { name, host, port } added by the player
  offlineName: '',           // dev mode only (~/.Valor/dev exists): the name to play offline with
};

function load() {
  let saved = {};
  try {
    saved = JSON.parse(fs.readFileSync(FILE, 'utf8'));
  } catch { /* first run, or unreadable: start from defaults */ }
  // Only Microsoft accounts that own the game can play; drop offline names and settings from earlier versions.
  saved.accounts = (saved.accounts || []).filter((a) => a.type === 'msa');
  delete saved.name;
  delete saved.clientId;
  if (typeof saved.ram !== 'number') delete saved.ram;   // it stored RAM as text such as "1G"
  return { ...DEFAULTS, ...saved };
}

let settings = load();

function save() {
  fs.mkdirSync(ROOT, { recursive: true });
  fs.writeFileSync(FILE, JSON.stringify(settings, null, 2));
}

function seal(text) {
  return safeStorage.isEncryptionAvailable() ? safeStorage.encryptString(text).toString('base64') : null;
}

function unseal(sealed) {
  try {
    return sealed ? safeStorage.decryptString(Buffer.from(sealed, 'base64')) : null;
  } catch {
    return null;
  }
}

module.exports = { get: () => settings, set(patch) { settings = { ...settings, ...patch }; save(); return settings; }, save, seal, unseal };
