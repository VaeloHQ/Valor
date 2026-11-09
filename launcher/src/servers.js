// Server list ping (Minecraft's status protocol), as the game's multiplayer menu does it.
const net = require('net');
const dns = require('dns').promises;

function varint(n) {
  const out = [];
  do {
    let b = n & 0x7f;
    n >>>= 7;
    if (n) b |= 0x80;
    out.push(b);
  } while (n);
  return Buffer.from(out);
}

function packet(id, payload) {
  const body = Buffer.concat([varint(id), payload]);
  return Buffer.concat([varint(body.length), body]);
}

function string(s) {
  const b = Buffer.from(s, 'utf8');
  return Buffer.concat([varint(b.length), b]);
}

/** Reads a varint at offset; returns [value, nextOffset] or null if the buffer ends first. */
function readVarint(buf, offset) {
  let value = 0;
  for (let i = 0; i < 5; i++) {
    if (offset + i >= buf.length) return null;
    const b = buf[offset + i];
    value |= (b & 0x7f) << (7 * i);
    if (!(b & 0x80)) return [value, offset + i + 1];
  }
  throw new Error('bad varint');
}

/** Turns a JSON chat component (or plain string) into text with § colour codes. */
const COLORS = { black: '0', dark_blue: '1', dark_green: '2', dark_aqua: '3', dark_red: '4', dark_purple: '5', gold: '6', gray: '7',
  dark_gray: '8', blue: '9', green: 'a', aqua: 'b', red: 'c', light_purple: 'd', yellow: 'e', white: 'f' };
function legacy(c) {
  if (typeof c === 'string') return c;
  if (!c) return '';
  let s = (c.color && COLORS[c.color] ? '§' + COLORS[c.color] : '') + (c.text || '');
  for (const e of c.extra || []) s += legacy(e);
  return s;
}

/** Resolves _minecraft._tcp SRV records like the game does. */
async function resolve(host, port) {
  if (port !== 25565) return { host, port };
  try {
    const [srv] = await dns.resolveSrv(`_minecraft._tcp.${host}`);
    if (srv) return { host: srv.name, port: srv.port };
  } catch { /* no SRV record */ }
  return { host, port };
}

/** Pings a server: { online, max, motd, favicon, version, latency }. Rejects on failure or after timeout ms. */
async function ping(address, port = 25565, timeout = 5000) {
  const target = await resolve(address, port);
  return new Promise((resolveP, reject) => {
    const socket = net.createConnection(target.port, target.host);
    let buf = Buffer.alloc(0);
    let status = null;
    let sentAt = 0;
    const fail = (e) => { socket.destroy(); reject(e); };
    socket.setTimeout(timeout, () => fail(new Error('timed out')));
    socket.on('error', fail);
    socket.on('connect', () => {
      const port16 = Buffer.alloc(2);
      port16.writeUInt16BE(target.port);
      socket.write(packet(0, Buffer.concat([varint(47), string(address), port16, varint(1)])));   // handshake, protocol 47 (1.8)
      socket.write(packet(0, Buffer.alloc(0)));   // status request
    });
    socket.on('data', (chunk) => {
      buf = Buffer.concat([buf, chunk]);
      for (;;) {
        const len = readVarint(buf, 0);
        if (!len || buf.length < len[1] + len[0]) return;
        const body = buf.subarray(len[1], len[1] + len[0]);
        buf = buf.subarray(len[1] + len[0]);
        const id = readVarint(body, 0);
        if (id[0] === 0 && !status) {
          const strLen = readVarint(body, id[1]);
          status = JSON.parse(body.subarray(strLen[1], strLen[1] + strLen[0]).toString('utf8'));
          const time = Buffer.alloc(8);
          time.writeBigInt64BE(BigInt(Date.now()));
          sentAt = Date.now();
          socket.write(packet(1, time));
        } else if (id[0] === 1 && status) {
          socket.destroy();
          resolveP({
            online: status.players ? status.players.online : 0,
            max: status.players ? status.players.max : 0,
            motd: legacy(status.description),
            favicon: status.favicon || null,
            version: status.version ? status.version.name : '',
            latency: Date.now() - sentAt,
          });
        }
      }
    });
  });
}

module.exports = { ping };
