// The only things the launcher page can ask the main process for.
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('valor', {
  settings: () => ipcRenderer.invoke('settings:get'),
  setSettings: (patch) => ipcRenderer.invoke('settings:set', patch),
  addMicrosoft: () => ipcRenderer.invoke('accounts:addMicrosoft'),
  selectAccount: (i) => ipcRenderer.invoke('accounts:select', i),
  removeAccount: (i) => ipcRenderer.invoke('accounts:remove', i),
  launch: (server) => ipcRenderer.invoke('launch', server),
  ping: (host, port) => ipcRenderer.invoke('servers:ping', host, port),
  news: () => ipcRenderer.invoke('news'),
  openGameFolder: () => ipcRenderer.invoke('openGameFolder'),
  window: (action) => ipcRenderer.send('window', action),
  onProgress: (fn) => ipcRenderer.on('progress', (e, p) => fn(p)),
  onGameExit: (fn) => ipcRenderer.on('game-exit', (e, code) => fn(code)),
});
