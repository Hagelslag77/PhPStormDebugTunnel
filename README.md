# PHP Debug Tunnel — PHPStorm Plugin

Automatically opens and closes an SSH reverse tunnel whenever you toggle
**Run → Start Listening for PHP Debug Connections** in PhpStorm.

No more manual `ssh -R ...` before every debug session.

---

## Requirements

| Tool | Notes |
|------|-------|
| JDK 21+ | For building only |
| Gradle 8.13+ | The wrapper downloads it automatically |
| PhpStorm 2025.1+ | Tested on 2025.3.3 |
| OpenSSH `ssh` / `ssh-add` | Built-in on macOS & Linux; use the Windows OpenSSH feature on Windows |

---

## Build

```bash
# First time only — download the Gradle wrapper binary
gradle wrapper          # requires Gradle to be installed once

# Build the plugin ZIP
./gradlew buildPlugin
```

The distributable ZIP is written to `build/distributions/php-tunnel-debug-plugin-1.0.0.zip`.

---

## Install

### From GitHub Releases (no build required)

1. Go to the [Releases](../../releases) page and download the latest `php-tunnel-debug-plugin-*.zip`.
2. Open PhpStorm.
3. **Settings → Plugins → ⚙ (gear icon) → Install Plugin from Disk…**
4. Select the downloaded ZIP.
5. Restart when prompted.

### From source

1. Build the plugin (see [Build](#build) below).
2. **Settings → Plugins → ⚙ (gear icon) → Install Plugin from Disk…**
3. Select the ZIP from `build/distributions/`.
4. Restart when prompted.

---

## Configure

**Settings → Tools → PHP Debug Tunnel**

| Field | Example | Description |
|-------|---------|-------------|
| SSH User | `root` | Leave empty to rely on `~/.ssh/config` |
| SSH Host | `example.com` | Remote hostname only |
| Remote Port | `9003` | Port opened on the remote server |
| Local Port | `9003` | Your local Xdebug port |
| Private Key | `~/.ssh/id_rsa` | Optional. If set, `ssh-add` is called before connecting |

### SSH Key / Agent setup

The plugin delegates authentication entirely to `ssh-agent`.

**macOS** — add your key to the agent once (stores it in the system keychain):
```bash
ssh-add --apple-use-keychain ~/.ssh/id_rsa
# On macOS 11 or older:
# ssh-add -K ~/.ssh/id_rsa
```

**Linux**:
```bash
ssh-add ~/.ssh/id_rsa
```

**Windows** — enable the OpenSSH Authentication Agent service, then:
```powershell
ssh-add $env:USERPROFILE\.ssh\id_rsa
```

If you fill in the *Private Key* field in settings, the plugin calls `ssh-add` automatically each time you start listening (safe to call even if the key is already loaded).

---

## How it works

The plugin registers an `AnActionListener` that fires before every IDE action.
When the action ID matches `PhpRuntime.StartDebugListen` (configurable under *Advanced* in settings):

- **Tunnel not running** → `ssh -R <remotePort>:localhost:<localPort> <user@host> -N` is started in the background; PhpStorm then starts listening for Xdebug connections.
- **Tunnel running** → the SSH process is killed; PhpStorm stops listening.

The SSH process is held as a `Process` object (no PID files) and is automatically destroyed when the IDE exits.

### SSH flags used

| Flag | Purpose |
|------|---------|
| `-N` | No remote command — only forward the port |
| `-o ExitOnForwardFailure=yes` | Fail immediately if the port cannot be forwarded |
| `-o ServerAliveInterval=30` | Send keep-alive packets every 30 s |
| `-o ServerAliveCountMax=3` | Kill the connection after 3 missed keep-alives |
| `-o StrictHostKeyChecking=accept-new` | Auto-accept new host keys; fail on changed keys |

---

## Troubleshooting

**Tunnel doesn't start / no notification**
- Confirm the action ID: **Help → Find Action**, type *Start Listening*, hover the result — the ID appears in the tooltip. Enter it in *Settings → Tools → PHP Debug Tunnel → Advanced → Listen Action ID*.
- Check **Help → Show Log in …** and filter for `SSH tunnel` or `PHP Debug Tunnel`.

**"SSH host is not configured" warning**
- Fill in the *SSH Host* field in settings.

**Port already in use on remote**
- Another tunnel may already be open. On the server: `ss -tlnp | grep <remotePort>` and kill the stale process.

**Windows: `ssh` not found**
- Enable **Optional Features → OpenSSH Client** in Windows Settings, or install [Git for Windows](https://git-scm.com/download/win) which bundles `ssh.exe`.
