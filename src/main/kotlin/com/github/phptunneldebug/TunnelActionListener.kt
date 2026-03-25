package com.github.phptunneldebug

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.diagnostic.logger

/**
 * Listens for the PHPStorm "Start Listening for PHP Debug Connections" action
 * (action ID: PhpListenDebugAction, configurable in Settings → Tools → PHP Debug Tunnel)
 * and starts or stops the SSH reverse tunnel accordingly.
 */
class TunnelActionListener : AnActionListener {

    private val log = logger<TunnelActionListener>()

    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
        val settings = TunnelSettings.getInstance().state
        if (!settings.enabled) return

        val actionId = ActionManager.getInstance().getId(action) ?: return
        if (actionId != settings.listenActionId) return

        log.info("PHP Debug Tunnel: matched action '$actionId', tunnel running=${TunnelManager.isRunning}")

        if (TunnelManager.isRunning) {
            TunnelManager.stop()
        } else {
            TunnelManager.start()
        }
    }
}
