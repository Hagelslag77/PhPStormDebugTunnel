package com.github.phptunneldebug

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.php.debug.listener.PhpDebugExternalConnectionsAccepter

/**
 * Runs once per project open. If the "Start Listening for PHP Debug Connections"
 * button is already active when PhpStorm starts, we start the SSH tunnel immediately
 * so that the tunnel state matches the IDE state from the very beginning.
 *
 * State is read from PhpDebugExternalConnectionsAccepter.isStarted, which is the
 * authoritative source used by the PHP plugin itself.
 */
class TunnelStartupActivity : ProjectActivity {

    private val log = logger<TunnelStartupActivity>()

    override suspend fun execute(project: Project) {
        val settings = TunnelSettings.getInstance().state
        if (!settings.enabled) return
        if (TunnelManager.isRunning) return

        val isListening = PhpDebugExternalConnectionsAccepter.getInstance(project).isStarted

        log.info("PHP Debug Tunnel startup sync: isListening=$isListening")

        if (isListening) {
            TunnelManager.start()
        }
    }
}
