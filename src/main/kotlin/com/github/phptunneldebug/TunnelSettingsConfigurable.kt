package com.github.phptunneldebug

import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class TunnelSettingsConfigurable : Configurable {

    // Local mirror of TunnelSettings.State used for bind* closures in the DSL panel.
    // Loaded from / saved to TunnelSettings in reset() / apply().
    private val form = TunnelSettings.State()

    private val uiPanel by lazy {
        panel {
            row {
                checkBox("Automatically manage SSH reverse tunnel when toggling PHP debug listening")
                    .bindSelected(form::enabled)
            }

            group("Connection") {
                row("SSH User:") {
                    textField()
                        .bindText(form::sshUser)
                        .columns(20)
                        .comment("e.g. <tt>root</tt> — leave empty to rely on <tt>~/.ssh/config</tt>")
                }
                row("SSH Host:") {
                    textField()
                        .bindText(form::sshHost)
                        .columns(32)
                        .comment("Hostname only, e.g. <tt>example.com</tt>")
                }
                row("Remote Port:") {
                    intTextField(1..65535)
                        .bindText(
                            getter = { form.remotePort.toString() },
                            setter = { form.remotePort = it.toIntOrNull() ?: 9003 }
                        )
                        .columns(8)
                        .comment("Port opened on the remote server, forwarded back to your machine")
                }
                row("Local Port:") {
                    intTextField(1..65535)
                        .bindText(
                            getter = { form.localPort.toString() },
                            setter = { form.localPort = it.toIntOrNull() ?: 9003 }
                        )
                        .columns(8)
                        .comment("Your local Xdebug port (usually <tt>9003</tt> for Xdebug 3, <tt>9000</tt> for Xdebug 2)")
                }
            }

            group("SSH Key (optional)") {
                row("Private Key:") {
                    textFieldWithBrowseButton(
                        fileChooserDescriptor = FileChooserDescriptor(
                            /* chooseFiles */ true,
                            /* chooseFolders */ false,
                            /* chooseJars */ false,
                            /* chooseJarsAsFiles */ false,
                            /* chooseJarContents */ false,
                            /* chooseMultiple */ false
                        ).withTitle("Select SSH Private Key")
                    )
                        .bindText(form::sshKeyPath)
                        .columns(36)
                        .comment(
                            "Leave empty to rely on your ssh-agent (recommended).<br>" +
                            "If set, <tt>ssh-add</tt> is called with this key before opening the tunnel.<br>" +
                            "On macOS the key is also stored in the system keychain " +
                            "(<tt>--apple-use-keychain</tt>)."
                        )
                }
            }

            collapsibleGroup("Advanced") {
                row("Listen Action ID:") {
                    textField()
                        .bindText(form::listenActionId)
                        .columns(36)
                        .comment(
                            "The PHPStorm action ID that triggers tunnel management.<br>" +
                            "Default: <tt>PhpListenDebugAction</tt><br>" +
                            "Find the correct ID via <b>Help &rarr; Find Action</b>, then check the keymap."
                        )
                }
            }
        }
    }

    override fun getDisplayName(): String = "PHP Debug Tunnel"

    override fun createComponent(): JComponent = uiPanel

    override fun isModified(): Boolean = uiPanel.isModified()

    override fun apply() {
        uiPanel.apply()           // DSL panel → form fields
        val s = TunnelSettings.getInstance().state
        s.enabled        = form.enabled
        s.sshUser        = form.sshUser.trim()
        s.sshHost        = form.sshHost.trim()
        s.remotePort     = form.remotePort
        s.localPort      = form.localPort
        s.sshKeyPath     = form.sshKeyPath.trim()
        s.listenActionId = form.listenActionId.trim()
    }

    override fun reset() {
        val s = TunnelSettings.getInstance().state
        form.enabled        = s.enabled
        form.sshUser        = s.sshUser
        form.sshHost        = s.sshHost
        form.remotePort     = s.remotePort
        form.localPort      = s.localPort
        form.sshKeyPath     = s.sshKeyPath
        form.listenActionId = s.listenActionId
        uiPanel.reset()           // form fields → DSL panel
    }
}
