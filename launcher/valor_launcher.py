"""Valor demo launcher: installs Minecraft 1.8.9 + Forge + Java 8 into ~/.Valor/game and launches with an offline name."""
import hashlib, io, json, os, platform, shutil, subprocess, sys, tarfile, urllib.request, uuid, zipfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

MC, FORGE = "1.8.9", "1.8.9-11.15.1.2318-1.8.9"
ROOT = Path.home() / ".Valor"
GAME = ROOT / "game"
LIBS, ASSETS = GAME / "libraries", GAME / "assets"
OS = {"Windows": "windows", "Darwin": "osx"}.get(platform.system(), "linux")
ARCH = {"amd64": "x64", "x86_64": "x64", "arm64": "aarch64", "aarch64": "aarch64"}.get(platform.machine().lower(), "x64")


def get(url, dest=None, sha1=None):
    """Download url (to dest if given). Skips files that already exist with the right hash."""
    if dest and dest.exists() and (not sha1 or hashlib.sha1(dest.read_bytes()).hexdigest() == sha1):
        return dest
    with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "ValorLauncher"}), timeout=60) as r:
        data = r.read()
    if sha1 and hashlib.sha1(data).hexdigest() != sha1:
        raise IOError(f"Bad hash: {url}")
    if not dest:
        return data
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(data)
    return dest


def allowed(lib):
    ok = not lib.get("rules")
    for r in lib.get("rules", []):
        if r.get("os", {}).get("name", OS) == OS:
            ok = r["action"] == "allow"
    return ok


def maven_path(name):
    g, a, v = name.split(":")
    return f"{g.replace('.', '/')}/{a}/{v}/{a}-{v}.jar"


def java8():
    jdir = ROOT / "runtime" / "java8"
    exe = next(jdir.glob("**/bin/" + ("javaw.exe" if OS == "windows" else "java")), None)
    if exe:
        return exe
    print("Downloading Java 8...")
    osn = {"windows": "windows", "osx": "mac"}.get(OS, "linux")
    data = get(f"https://api.adoptium.net/v3/binary/latest/8/ga/{osn}/{ARCH}/jre/hotspot/normal/eclipse")
    jdir.mkdir(parents=True, exist_ok=True)
    if OS == "windows":
        zipfile.ZipFile(io.BytesIO(data)).extractall(jdir)
    else:
        tarfile.open(fileobj=io.BytesIO(data)).extractall(jdir, **({"filter": "data"} if hasattr(tarfile, "data_filter") else {}))
    return java8()


def install():
    manifest = json.loads(get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    vinfo = next(v for v in manifest["versions"] if v["id"] == MC)
    ver = json.loads(get(vinfo["url"], GAME / "versions" / MC / f"{MC}.json", vinfo["sha1"]).read_text())
    jobs = [(ver["downloads"]["client"]["url"], GAME / "versions" / MC / f"{MC}.jar", ver["downloads"]["client"]["sha1"])]

    # Forge: version info and universal jar both live inside the installer.
    inst = zipfile.ZipFile(get(f"https://maven.minecraftforge.net/net/minecraftforge/forge/{FORGE}/forge-{FORGE}-installer.jar",
                               GAME / "versions" / FORGE / "installer.jar"))
    prof = json.loads(inst.read("install_profile.json"))
    fjar = LIBS / maven_path(prof["install"]["path"])
    if not fjar.exists():
        fjar.parent.mkdir(parents=True, exist_ok=True)
        fjar.write_bytes(inst.read(prof["install"]["filePath"]))

    cp, natives = {}, []
    for lib in ver["libraries"]:
        if not allowed(lib):
            continue
        d = lib.get("downloads", {})
        if "artifact" in d:
            a = d["artifact"]
            cp[lib["name"].rsplit(":", 1)[0]] = LIBS / a["path"]
            jobs.append((a["url"], LIBS / a["path"], a["sha1"]))
        if OS in lib.get("natives", {}):
            n = d["classifiers"][lib["natives"][OS].replace("${arch}", "64")]
            natives.append(LIBS / n["path"])
            jobs.append((n["url"], LIBS / n["path"], n["sha1"]))
    for lib in prof["versionInfo"]["libraries"]:
        if lib.get("clientreq") is False:
            continue
        path = LIBS / maven_path(lib["name"])
        cp[lib["name"].rsplit(":", 1)[0]] = path
        if path != fjar:
            base = lib.get("url", "https://libraries.minecraft.net/").replace("http://files.minecraftforge.net/maven/", "https://maven.minecraftforge.net/")
            jobs.append((base + maven_path(lib["name"]), path, None))

    idx = ver["assetIndex"]
    objs = json.loads(get(idx["url"], ASSETS / "indexes" / f"{idx['id']}.json", idx["sha1"]).read_text())["objects"]
    for o in objs.values():
        h = o["hash"]
        jobs.append((f"https://resources.download.minecraft.net/{h[:2]}/{h}", ASSETS / "objects" / h[:2] / h, h))
    log = ver.get("logging", {}).get("client", {}).get("file")  # Log4Shell-safe log config from Mojang
    if log:
        jobs.append((log["url"], ASSETS / "log_configs" / log["id"], log["sha1"]))

    print(f"Checking {len(jobs)} files...")
    with ThreadPoolExecutor(16) as ex:
        list(ex.map(lambda j: get(*j), jobs))

    nat = GAME / "versions" / FORGE / "natives"
    nat.mkdir(parents=True, exist_ok=True)
    for n in natives:
        with zipfile.ZipFile(n) as z:
            for e in z.namelist():
                if not e.startswith("META-INF") and not e.endswith("/"):
                    (nat / Path(e).name).write_bytes(z.read(e))
    return ver, prof["versionInfo"], list(cp.values()) + [GAME / "versions" / MC / f"{MC}.jar"], nat, log


def sync_mod():
    """Copy the newest valor-*.jar from ~/.Valor into the mods folder."""
    jars = sorted(ROOT.glob("valor-*.jar"), key=os.path.getmtime)
    mods = GAME / "mods"
    mods.mkdir(parents=True, exist_ok=True)
    if jars:
        for old in mods.glob("valor-*.jar"):
            old.unlink()
        shutil.copy2(jars[-1], mods)
    elif not any(mods.glob("valor-*.jar")):
        print(f"Warning: no valor-*.jar found. Put it in {ROOT}")


def offline_uuid(name):
    """Same UUID vanilla gives an offline player (UUID.nameUUIDFromBytes)."""
    h = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
    h[6], h[8] = h[6] & 0x0F | 0x30, h[8] & 0x3F | 0x80
    return uuid.UUID(bytes=bytes(h)).hex


def main():
    cfg_file = ROOT / "launcher.json"
    cfg = json.loads(cfg_file.read_text()) if cfg_file.exists() else {}
    if "name" not in cfg:
        cfg["name"] = input("Username: ").strip() or "ValorPlayer"
        cfg.setdefault("ram", "1G")
        ROOT.mkdir(parents=True, exist_ok=True)
        cfg_file.write_text(json.dumps(cfg, indent=2))

    java = java8()
    ver, forge, cp, nat, log = install()
    sync_mod()

    name = cfg["name"]
    sub = {"auth_player_name": name, "version_name": FORGE, "game_directory": str(GAME), "assets_root": str(ASSETS),
           "assets_index_name": ver["assetIndex"]["id"], "auth_uuid": offline_uuid(name),
           "auth_access_token": "0", "user_properties": "{}", "user_type": "legacy"}
    args = [sub.get(a[2:-1], a) if a.startswith("${") else a for a in forge["minecraftArguments"].split()]
    cmd = [str(java), f"-Xmx{cfg['ram']}", f"-Djava.library.path={nat}", "-cp", os.pathsep.join(map(str, cp))]
    if log:
        cmd.append(ver["logging"]["client"]["argument"].replace("${path}", str(ASSETS / "log_configs" / log["id"])))
    cmd += [forge["mainClass"]] + args + (sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else [])  # extra game args, e.g. -- --width 1280 --height 720
    print("Launching Valor...")
    if "--dry-run" in sys.argv:
        print(" ".join(cmd))
        return
    subprocess.run(cmd, cwd=GAME)


if __name__ == "__main__":
    main()
