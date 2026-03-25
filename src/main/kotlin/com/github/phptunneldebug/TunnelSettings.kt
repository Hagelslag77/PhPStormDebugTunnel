package com.github.phptunneldebug

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(
    name = "PhpTunnelDebugSettings",
    storages = [Storage("php-tunnel-debug.xml")]
)
class TunnelSettings : PersistentStateComponent<TunnelSettings.State> {

    data class State(
        var enabled: Boolean = true,
        var sshUser: String = "",
        var sshHost: String = "",
        var remotePort: Int = 9003,
        var localPort: Int = 9003,
        // Optional: if non-empty, ssh-add is run before opening the tunnel.
        var sshKeyPath: String = "",
        // The PHPStorm action ID to hook into. Rarely needs changing.
        var listenActionId: String = "PhpListenDebugAction"
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    companion object {
        fun getInstance(): TunnelSettings = service()
    }
}
