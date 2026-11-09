// Installs Minecraft 1.8.9 + Forge + Java 8 into ~/.Valor/game, then launches it with the Valor mod.
const fs = require('fs');
const fsp = fs.promises;
const path = require('path');
const os = require('os');
const crypto = require('crypto');
const { spawn } = require('child_process');
const AdmZip = require('adm-zip');
const tar = require('tar');

const MC = '1.8.9';
const FORGE = '1.8.9-11.15.1.2318-1.8.9';
const ROOT = path.join(os.homedir(), '.Valor');
const GAME = path.join(ROOT, 'game');
const LIBS = path.join(GAME, 'libraries');
const ASSETS = path.join(GAME, 'assets');
const OS = { win32: 'windows', darwin: 'osx' }[process.platform] || 'linux';
const ARCH = { x64: 'x64', arm64: 'aarch64' }[process.arch] || 'x64';

const sha1 = (data) => crypto.createHash('sha1').update(data).digest('hex');

async function exists(p) {
  try { await fsp.access(p); return true; } catch { return false; }
}

/** Downloads url (to dest if given), retrying twice. Skips files that already exist with the right hash. */
async function get(url, dest, hash) {
  if (dest && await exists(dest) && (!hash || sha1(await fsp.readFile(dest)) === hash)) return dest;
  let lastError;
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const res = await fetch(url, { headers: { 'User-Agent': 'ValorLauncher' } });
      if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
      const data = Buffer.from(await res.arrayBuffer());
      if (hash && sha1(data) !== hash) throw new Error(`Bad hash for ${url}`);
      if (!dest) return data;
      await fsp.mkdir(path.dirname(dest), { recursive: true });
      await fsp.writeFile(dest, data);
      return dest;
    } catch (e) {
      lastError = e;
    }
  }
  throw lastError;
}

function allowed(lib) {
  let ok = !lib.rules;
  for (const r of lib.rules || []) if ((r.os ? r.os.name : OS) === OS) ok = r.action === 'allow';
  return ok;
}

function mavenPath(name) {
  const [g, a, v] = name.split(':');
  return `${g.replace(/\./g, '/')}/${a}/${v}/${a}-${v}.jar`;
}

async function findJava(dir) {
  const exe = OS === 'windows' ? 'javaw.exe' : 'java';
  if (!await exists(dir)) return null;
  for (const entry of await fsp.readdir(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'bin' && await exists(path.join(p, exe))) return path.join(p, exe);
      const found = await findJava(p);
      if (found) return found;
    }
  }
  return null;
}

/** Java 8 from Adoptium, downloaded once into ~/.Valor/runtime. */
async function java8(progress) {
  const dir = path.join(ROOT, 'runtime', 'java8');
  const found = await findJava(dir);
  if (found) return found;
  progress('Downloading Java 8', 0, 1);
  const osName = { windows: 'windows', osx: 'mac' }[OS] || 'linux';
  const data = await get(`https://api.adoptium.net/v3/binary/latest/8/ga/${osName}/${ARCH}/jre/hotspot/normal/eclipse`);
  await fsp.mkdir(dir, { recursive: true });
  if (OS === 'windows') new AdmZip(data).extractAllTo(dir, true);
  else {
    const archive = path.join(dir, 'jre.tar.gz');
    await fsp.writeFile(archive, data);
    await tar.x({ file: archive, cwd: dir });
    await fsp.unlink(archive);
  }
  return findJava(dir);
}

/** Downloads everything Minecraft and Forge need; returns what launching needs. */
async function install(progress) {
  progress('Checking game files', 0, 1);
  const verFile = path.join(GAME, 'versions', MC, `${MC}.json`);
  let ver;
  try {
    const manifest = JSON.parse(await get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'));
    const vinfo = manifest.versions.find((v) => v.id === MC);
    ver = JSON.parse(await fsp.readFile(await get(vinfo.url, verFile, vinfo.sha1), 'utf8'));
  } catch (e) {
    // Offline: play with the files already installed (every download below is skipped when its file is there).
    if (!await exists(verFile)) throw new Error("Couldn't download Minecraft. Check your internet connection.");
    ver = JSON.parse(await fsp.readFile(verFile, 'utf8'));
  }
  const clientJar = path.join(GAME, 'versions', MC, `${MC}.jar`);
  const jobs = [[ver.downloads.client.url, clientJar, ver.downloads.client.sha1]];

  // Forge: its version info and universal jar both live inside the installer.
  const installer = new AdmZip(await get(`https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE}/forge-${FORGE}-installer.jar`,
    path.join(GAME, 'versions', FORGE, 'installer.jar')));
  const prof = JSON.parse(installer.readAsText('install_profile.json'));
  const forgeJar = path.join(LIBS, mavenPath(prof.install.path));
  if (!await exists(forgeJar)) {
    await fsp.mkdir(path.dirname(forgeJar), { recursive: true });
    await fsp.writeFile(forgeJar, installer.readFile(prof.install.filePath));
  }

  const cp = new Map();
  const natives = [];
  for (const lib of ver.libraries) {
    if (!allowed(lib)) continue;
    const d = lib.downloads || {};
    if (d.artifact) {
      cp.set(lib.name.split(':').slice(0, 2).join(':'), path.join(LIBS, d.artifact.path));
      jobs.push([d.artifact.url, path.join(LIBS, d.artifact.path), d.artifact.sha1]);
    }
    if (lib.natives && lib.natives[OS]) {
      const n = d.classifiers[lib.natives[OS].replace('${arch}', '64')];
      natives.push(path.join(LIBS, n.path));
      jobs.push([n.url, path.join(LIBS, n.path), n.sha1]);
    }
  }
  for (const lib of prof.versionInfo.libraries) {
    if (lib.clientreq === false) continue;
    const p = path.join(LIBS, mavenPath(lib.name));
    cp.set(lib.name.split(':').slice(0, 2).join(':'), p);
    if (p !== forgeJar) {
      const base = (lib.url || 'https://libraries.minecraft.net/').replace('http://files.minecraftforge.net/maven/', 'https://maven.minecraftforge.net/');
      jobs.push([base + mavenPath(lib.name), p, null]);
    }
  }

  const idx = ver.assetIndex;
  const objects = JSON.parse(await fsp.readFile(await get(idx.url, path.join(ASSETS, 'indexes', `${idx.id}.json`), idx.sha1), 'utf8')).objects;
  for (const o of Object.values(objects))
    jobs.push([`https://resources.download.minecraft.net/${o.hash.slice(0, 2)}/${o.hash}`, path.join(ASSETS, 'objects', o.hash.slice(0, 2), o.hash), o.hash]);
  const log = ver.logging && ver.logging.client && ver.logging.client.file;   // Log4Shell-safe log config from Mojang
  if (log) jobs.push([log.url, path.join(ASSETS, 'log_configs', log.id), log.sha1]);

  let done = 0;
  const queue = jobs.slice();
  const worker = async () => {
    for (let job; (job = queue.shift());) {
      await get(...job);
      progress('Downloading game files', ++done, jobs.length);
    }
  };
  await Promise.all(Array.from({ length: 16 }, worker));

  const nativesDir = path.join(GAME, 'versions', FORGE, 'natives');
  await fsp.mkdir(nativesDir, { recursive: true });
  for (const n of natives)
    for (const e of new AdmZip(n).getEntries())
      if (!e.entryName.startsWith('META-INF') && !e.isDirectory) await fsp.writeFile(path.join(nativesDir, path.basename(e.entryName)), e.getData());
  return { ver, forge: prof.versionInfo, classpath: [...cp.values(), clientJar], nativesDir, log };
}

/** Puts the Valor mod into the mods folder, replacing older copies. */
async function syncMod(jar) {
  const mods = path.join(GAME, 'mods');
  await fsp.mkdir(mods, { recursive: true });
  if (!jar || !await exists(jar)) return;
  for (const f of await fsp.readdir(mods)) if (/^valor-.*\.jar$/.test(f)) await fsp.unlink(path.join(mods, f));
  await fsp.copyFile(jar, path.join(mods, 'valor-' + require('../package.json').version + '.jar'));
}

/** Forge's own loading screen runs before mods load; turning it off lets Valor's loading screen show from the first launch. */
async function splashOff() {
  const file = path.join(GAME, 'config', 'splash.properties');
  await fsp.mkdir(path.dirname(file), { recursive: true });
  let text = await exists(file) ? await fsp.readFile(file, 'utf8') : '';
  if (/^enabled=false$/m.test(text)) return;
  text = /^enabled=/m.test(text) ? text.replace(/^enabled=.*$/m, 'enabled=false') : text + 'enabled=false\n';
  await fsp.writeFile(file, text);
}

/** The UUID vanilla gives an offline player (UUID.nameUUIDFromBytes("OfflinePlayer:" + name)). */
function offlineUuid(name) {
  const h = crypto.createHash('md5').update('OfflinePlayer:' + name).digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  return h.toString('hex');
}

/**
 * Installs if needed and starts the game. account: { name, uuid, token, type } ('msa' or 'offline').
 * options: { ram (MB), width, height, fullscreen, javaPath, jvmArgs, server: { host, port } }. Returns the child process.
 */
async function launch(account, options, modJar, progress) {
  const java = options.javaPath || await java8(progress);
  const { ver, forge, classpath, nativesDir, log } = await install(progress);
  progress('Starting Valor', 1, 1);
  await syncMod(modJar);
  await splashOff();
  const sub = {
    auth_player_name: account.name, version_name: FORGE, game_directory: GAME, assets_root: ASSETS,
    assets_index_name: ver.assetIndex.id, auth_uuid: account.type === 'msa' ? account.uuid : offlineUuid(account.name),
    auth_access_token: account.type === 'msa' ? account.token : '0', user_properties: '{}', user_type: account.type === 'msa' ? 'msa' : 'legacy',
  };
  const gameArgs = forge.minecraftArguments.split(' ').map((a) => (a.startsWith('${') ? sub[a.slice(2, -1)] ?? a : a));
  if (options.fullscreen) gameArgs.push('--fullscreen');
  else gameArgs.push('--width', String(options.width), '--height', String(options.height));
  if (options.server) gameArgs.push('--server', options.server.host, '--port', String(options.server.port));
  const jvm = [`-Xmx${options.ram}M`, '-XX:+UseG1GC', '-XX:MaxGCPauseMillis=50', `-Djava.library.path=${nativesDir}`,
    ...(options.jvmArgs ? options.jvmArgs.split(/\s+/).filter(Boolean) : []), '-cp', classpath.join(path.delimiter)];
  if (log) jvm.push(ver.logging.client.argument.replace('${path}', path.join(ASSETS, 'log_configs', log.id)));
  // Output goes to a file (not a pipe) and the game gets its own process group, so it keeps running if the launcher closes.
  const out = fs.openSync(path.join(ROOT, 'game-output.log'), 'w');
  const child = spawn(java, [...jvm, forge.mainClass, ...gameArgs], { cwd: GAME, detached: true, stdio: ['ignore', out, out], windowsHide: false });
  fs.closeSync(out);
  return child;
}

module.exports = { launch, ROOT, GAME, offlineUuid };
