// Microsoft sign-in for Minecraft: Microsoft account -> Xbox Live -> XSTS -> Minecraft services.
// Needs an Azure app (public client) approved for the Minecraft API; its client ID is set in the launcher settings.
const { BrowserWindow } = require('electron');
const crypto = require('crypto');

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

async function msToken(clientId, params) {
  const res = await fetch(`${MS}/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ client_id: clientId, scope: SCOPE, redirect_uri: REDIRECT, ...params }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(json.error_description || 'Microsoft sign-in failed.');
  return json;
}

/** Opens Microsoft's sign-in page in a window and resolves with the authorization code (PKCE). */
function interactiveCode(parent, clientId, challenge) {
  return new Promise((resolve, reject) => {
    const win = new BrowserWindow({ width: 500, height: 640, parent, modal: true, autoHideMenuBar: true, title: 'Sign in with Microsoft',
      webPreferences: { partition: 'auth', contextIsolation: true, nodeIntegration: false } });
    let settled = false;
    const finish = (fn, value) => {
      if (settled) return;
      settled = true;
      win.destroy();
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
    win.on('closed', () => finish(reject, new Error('Sign-in was cancelled.')));
    const q = new URLSearchParams({ client_id: clientId, response_type: 'code', redirect_uri: REDIRECT, scope: SCOPE,
      code_challenge: challenge, code_challenge_method: 'S256', prompt: 'select_account' });
    win.loadURL(`${MS}/authorize?${q}`);
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
  const mc = await postJson('https://api.minecraftservices.com/authentication/login_with_xbox', { identityToken: `XBL3.0 x=${uhs};${xsts.Token}` });
  const res = await fetch('https://api.minecraftservices.com/minecraft/profile', { headers: { Authorization: `Bearer ${mc.access_token}` } });
  if (res.status === 404) throw new Error("This Microsoft account doesn't own Minecraft: Java Edition.");
  if (!res.ok) throw new Error('Couldn\'t load the Minecraft profile.');
  const profile = await res.json();
  return { type: 'msa', uuid: profile.id, name: profile.name, token: mc.access_token, expires: Date.now() + (mc.expires_in - 60) * 1000 };
}

/** Full sign-in with a window. Returns the account plus the Microsoft refresh token to keep. */
async function signIn(parent, clientId) {
  const verifier = base64url(crypto.randomBytes(32));
  const challenge = base64url(crypto.createHash('sha256').update(verifier).digest());
  const code = await interactiveCode(parent, clientId, challenge);
  const ms = await msToken(clientId, { grant_type: 'authorization_code', code, code_verifier: verifier });
  return { account: await minecraft(ms.access_token), refresh: ms.refresh_token };
}

/** Silent refresh from a stored Microsoft refresh token (Minecraft tokens last a day). */
async function refresh(clientId, refreshToken) {
  const ms = await msToken(clientId, { grant_type: 'refresh_token', refresh_token: refreshToken });
  return { account: await minecraft(ms.access_token), refresh: ms.refresh_token || refreshToken };
}

module.exports = { signIn, refresh };
