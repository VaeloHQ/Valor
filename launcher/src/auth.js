// Microsoft sign-in for Minecraft: Microsoft account -> Xbox Live -> XSTS -> Minecraft services.
const { BrowserWindow } = require('electron');
const crypto = require('crypto');

/** Valor's Azure app (public client, personal Microsoft accounts). A client ID is public, not a secret. */
const CLIENT_ID = 'f33b6a77-918e-47d4-919c-6d866b6ca339';

const REDIRECT = 'https://login.microsoftonline.com/common/oauth2/nativeclient';
const SCOPE = 'XboxLive.signin offline_access';
const MS = 'https://login.microsoftonline.com/consumers/oauth2/v2.0';

const base64url = (buf) => buf.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

async function postJson(url, body, headers = {}) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...headers },
    body: JSON.stringify(body),
  });
  const text = await res.text();
  let json = null;
  try { json = text ? JSON.parse(text) : null; } catch { /* not JSON */ }
  if (!res.ok) {
    const err = new Error(`${url} answered ${res.status}`);
    err.status = res.status;
    err.body = json;
    throw err;
  }
  return json;
}

async function msToken(params) {
  const res = await fetch(`${MS}/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ client_id: CLIENT_ID, scope: SCOPE, redirect_uri: REDIRECT, ...params }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.error_description || 'Microsoft sign-in failed.');
  return json;
}

/** Opens Microsoft's sign-in page in a window and resolves with the authorization code (PKCE). */
function interactiveCode(parent, challenge) {
  return new Promise((resolve, reject) => {
    const win = new BrowserWindow({ width: 500, height: 640, parent, modal: true, autoHideMenuBar: true, title: 'Sign in with Microsoft',
      webPreferences: { partition: 'auth', contextIsolation: true, nodeIntegration: false } });
    let settled = false;
    const finish = (fn, value) => {
      if (settled) return;
      settled = true;
      if (!win.isDestroyed()) win.destroy();
      fn(value);
    };
    const check = (url) => {
      if (!url.startsWith(REDIRECT)) return;
      const q = new URL(url).searchParams;
      if (q.get('code')) finish(resolve, q.get('code'));
      else finish(reject, new Error(q.get('error_description') || 'Sign-in was cancelled.'));
    };
    win.webContents.on('will-redirect', (e, url) => check(url));
    win.webContents.on('will-navigate', (e, url) => check(url));
    win.webContents.on('did-fail-load', (e, code, text, url, mainFrame) => {
      if (mainFrame && code !== -3) finish(reject, new Error("Couldn't reach Microsoft. Check your internet connection."));   // -3: aborted
    });
    win.on('closed', () => finish(reject, new Error('Sign-in was cancelled.')));
    const q = new URLSearchParams({ client_id: CLIENT_ID, response_type: 'code', redirect_uri: REDIRECT, scope: SCOPE,
      code_challenge: challenge, code_challenge_method: 'S256', prompt: 'select_account' });
    win.loadURL(`${MS}/authorize?${q}`).catch(() => { /* reported by did-fail-load */ });
  });
}

const XSTS_ERRORS = {
  2148916233: 'This Microsoft account has no Xbox profile. Sign in at xbox.com once, then try again.',
  2148916235: 'Xbox Live is not available in your country.',
  2148916236: 'This account needs adult verification on xbox.com.',
  2148916237: 'This account needs adult verification on xbox.com.',
  2148916238: 'This is a child account. An adult must add it to a Microsoft family first.',
};

/** Microsoft access token -> Minecraft token and profile. */
async function minecraft(msAccess) {
  const xbl = await postJson('https://user.auth.xboxlive.com/user/authenticate', {
    Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: `d=${msAccess}` },
    RelyingParty: 'http://auth.xboxlive.com', TokenType: 'JWT',
  });
  let xsts;
  try {
    xsts = await postJson('https://xsts.auth.xboxlive.com/xsts/authorize', {
      Properties: { SandboxId: 'RETAIL', UserTokens: [xbl.Token] }, RelyingParty: 'rp://api.minecraftservices.com/', TokenType: 'JWT',
    });
  } catch (e) {
    throw new Error((e.body && XSTS_ERRORS[e.body.XErr]) || 'Xbox Live sign-in failed.');
  }
  const uhs = xsts.DisplayClaims.xui[0].uhs;
  let mc;
  try {
    mc = await postJson('https://api.minecraftservices.com/authentication/login_with_xbox', { identityToken: `XBL3.0 x=${uhs};${xsts.Token}` });
  } catch (e) {
    // Minecraft refuses apps Microsoft hasn't approved yet with 403.
    throw new Error(e.status === 403 ? 'Valor is waiting for Microsoft to approve it for Minecraft sign-in. Try again later.'
      : 'Minecraft sign-in failed. Try again in a moment.');
  }
  const res = await fetch('https://api.minecraftservices.com/minecraft/profile', { headers: { Authorization: `Bearer ${mc.access_token}` } });
  if (res.status === 404) throw new Error("This Microsoft account doesn't own Minecraft: Java Edition.");
  if (!res.ok) throw new Error('Couldn\'t load the Minecraft profile.');
  const profile = await res.json();
  return { type: 'msa', uuid: profile.id, name: profile.name, token: mc.access_token, expires: Date.now() + (mc.expires_in - 60) * 1000 };
}

/** Full sign-in with a window. Returns the account plus the Microsoft refresh token to keep. */
async function signIn(parent) {
  const verifier = base64url(crypto.randomBytes(32));
  const challenge = base64url(crypto.createHash('sha256').update(verifier).digest());
  const code = await interactiveCode(parent, challenge);
  const ms = await msToken({ grant_type: 'authorization_code', code, code_verifier: verifier });
  return { account: await minecraft(ms.access_token), refresh: ms.refresh_token };
}

/** Silent refresh from a stored Microsoft refresh token (Minecraft tokens last a day). */
async function refresh(refreshToken) {
  const ms = await msToken({ grant_type: 'refresh_token', refresh_token: refreshToken });
  return { account: await minecraft(ms.access_token), refresh: ms.refresh_token || refreshToken };
}

module.exports = { signIn, refresh };
