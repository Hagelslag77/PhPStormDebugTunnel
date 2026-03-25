package com.github.phptunneldebug

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import java.io.File

object TunnelManager {

    private val log = logger<TunnelManager>()

    @Volatile
    private var process: Process? = null

    val isRunning: Boolean
        get() = process?.isAlive == true

    init {
        // Ensure the SSH process is killed when the IDE shuts down.
        Runtime.getRuntime().addShutdownHook(Thread {
            process?.destroy()
        })
    }

    /** Start the SSH reverse tunnel. No-op if already running. */
    fun start() {
        if (isRunning) return

        val s = TunnelSettings.getInstance().state
        if (!s.enabled) return

        if (s.sshHost.isBlank()) {
            notify(
                "SSH host is not configured. Go to Settings → Tools → PHP Debug Tunnel.",
                NotificationType.WARNING
            )
            return
        }

        // Run off the EDT so we don't block the UI.
        ApplicationManager.getApplication().executeOnPooledThread {
            if (s.sshKeyPath.isNotBlank()) runSshAdd(s.sshKeyPath)
            launchSsh(s)
        }
    }

    /** Stop the SSH reverse tunnel. No-op if not running. */
    fun stop() {
        val proc = process ?: return
        process = null
        proc.destroy()
        notify("PHP Debug Tunnel stopped.", NotificationType.INFORMATION)
    }

    // -------------------------------------------------------------------------

    private fun launchSsh(s: TunnelSettings.State) {
        val sshBin = findSshExecutable()
        val target = buildString {
            if (s.sshUser.isNotBlank()) append("${s.sshUser}@")
            append(s.sshHost)
        }

        val cmd = buildList {
            add(sshBin)
            add("-R"); add("${s.remotePort}:localhost:${s.localPort}")
            add(target)
            add("-N")
            // Specify key explicitly if configured.
            if (s.sshKeyPath.isNotBlank()) {
                val expanded = s.sshKeyPath.replace("~", System.getProperty("user.home"))
                add("-i"); add(expanded)
            }
            add("-o"); add("ExitOnForwardFailure=yes")
            add("-o"); add("ServerAliveInterval=30")
            add("-o"); add("ServerAliveCountMax=3")
            add("-o"); add("StrictHostKeyChecking=accept-new")
        }

        log.info("Starting SSH tunnel: ${cmd.joinToString(" ")}")

        try {
            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
            process = proc

            // Capture stderr/stdout so we can surface errors in the IDE log and
            // in a notification if SSH exits non-zero.
            val output = StringBuilder()
            Thread {
                proc.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        log.info("SSH tunnel: $line")
                        output.appendLine(line)
                    }
                }
            }.apply { isDaemon = true; start() }

            // Watch for unexpected exits.
            Thread {
                val exitCode = proc.waitFor()
                if (process === proc) {
                    process = null
                    if (exitCode != 0) {
                        val detail = output.toString().trim().take(200).ifBlank { "no output" }
                        notify(
                            "PHP Debug Tunnel exited (code $exitCode): $detail",
                            NotificationType.ERROR
                        )
                    }
                }
            }.apply { isDaemon = true; start() }

            notify(
                "PHP Debug Tunnel started: ${target}:${s.remotePort} → localhost:${s.localPort}",
                NotificationType.INFORMATION
            )
        } catch (e: Exception) {
            log.error("Failed to start SSH tunnel", e)
            notify("Failed to start SSH tunnel: ${e.message}", NotificationType.ERROR)
        }
    }

    private fun runSshAdd(keyPath: String) {
        val expanded = keyPath.replace("~", System.getProperty("user.home"))
        if (!File(expanded).exists()) {
            notify("SSH key not found: $expanded", NotificationType.WARNING)
            return
        }

        // macOS stores keys in the system keychain; --apple-use-keychain is the
        // current flag (replaces the old -K in macOS 12+).
        val cmd: List<String> = when {
            isMacOs() -> listOf("ssh-add", "--apple-use-keychain", expanded)
            else      -> listOf("ssh-add", expanded)
        }

        try {
            val code = ProcessBuilder(cmd).start().waitFor()
            if (code != 0 && isMacOs()) {
                // Fallback for older macOS that uses -K
                ProcessBuilder("ssh-add", "-K", expanded).start().waitFor()
            }
        } catch (e: Exception) {
            // Not fatal – key may already be loaded in the agent.
            log.warn("ssh-add failed (the key may already be loaded): ${e.message}")
        }
    }

    /** Locate the ssh binary. On Windows we prefer the built-in OpenSSH location. */
    private fun findSshExecutable(): String {
        if (isWindows()) {
            val winBuiltIn = File("C:\\Windows\\System32\\OpenSSH\\ssh.exe")
            if (winBuiltIn.exists()) return winBuiltIn.absolutePath
            // Git for Windows ships its own ssh; look in the usual location.
            val gitSsh = File("C:\\Program Files\\Git\\usr\\bin\\ssh.exe")
            if (gitSsh.exists()) return gitSsh.absolutePath
        }
        return "ssh"
    }

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("windows")
    private fun isMacOs()   = System.getProperty("os.name").lowercase().contains("mac")

    private fun notify(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("PHP Debug Tunnel")
            .createNotification("PHP Debug Tunnel", content, type)
            .notify(null)
    }
}
