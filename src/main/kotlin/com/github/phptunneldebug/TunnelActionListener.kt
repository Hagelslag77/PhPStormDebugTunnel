package com.github.phptunneldebug

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.php.debug.listener.PhpDebugExternalConnectionsAccepter

/**
 * Listens for the PHPStorm "Start Listening for PHP Debug Connections" action
 * (action ID: PhpListenDebugAction, configurable in Settings → Tools → PHP Debug Tunnel)
 * and starts or stops the SSH reverse tunnel accordingly.
 *
 * We use afterActionPerformed and read PhpDebugExternalConnectionsAccepter.isStarted()
 * to get the authoritative post-toggle state from the PHP plugin itself.
 * PhpListenDebugAction extends AnAction (not ToggleAction), so isStarted() is the
 * only reliable way to query whether the debugger is now listening.
 */
class TunnelActionListener : AnActionListener {

    private val log = logger<TunnelActionListener>()

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        val settings = TunnelSettings.getInstance().state
        if (!settings.enabled) return

        val actionId = ActionManager.getInstance().getId(action) ?: return
        if (actionId != settings.listenActionId) return

        val project = event.project ?: return
        val isListening = PhpDebugExternalConnectionsAccepter.getInstance(project).isStarted

        log.info("PHP Debug Tunnel: action '$actionId' performed, isListening=$isListening, tunnel running=${TunnelManager.isRunning}")

        if (isListening && !TunnelManager.isRunning) {
            TunnelManager.start()
        } else if (!isListening && TunnelManager.isRunning) {
            TunnelManager.stop()
        }
    }
}
