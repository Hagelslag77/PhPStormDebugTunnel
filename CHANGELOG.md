# PhPStorm Debug Tunnel Plugin changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-03-25

### Added
- Automatic SSH reverse tunnel management tied to PhpStorm's **Start Listening for PHP Debug Connections** action.
- `TunnelSettings` — persistent app-level settings (SSH user, host, remote port, local port, private key path, listen action ID).
- `TunnelManager` — singleton managing the SSH process lifecycle; process is destroyed automatically on IDE exit.
- `TunnelActionListener` — `AnActionListener` intercepting `PhpListenDebugAction` to start/stop the tunnel in sync with the debugger.
- `TunnelSettingsConfigurable` — settings UI under **Settings → Tools → PHP Debug Tunnel**.
- SSH keep-alive (`ServerAliveInterval=30`, `ServerAliveCountMax=3`) and `ExitOnForwardFailure=yes` for reliable tunnel detection.
- Optional private key field: calls `ssh-add` automatically before connecting.
- GitHub Actions workflow to build and publish the plugin ZIP on version tags.

