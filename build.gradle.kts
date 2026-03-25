plugins {
    id("org.jetbrains.intellij.platform") version "2.11.0"
    kotlin("jvm") version "2.0.21"
}

group = "com.github.phptunneldebug"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Target PHPStorm. Change the version string to match the oldest version you want to support.
        phpstorm("2025.1")
        bundledPlugin("com.jetbrains.php")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "PHP Debug Tunnel"
        version = project.version.toString()
        ideaVersion {
            // Build 251 = 2025.1. Leaving untilBuild unset allows the plugin to run on future versions.
            sinceBuild = "251"
            untilBuild = provider { null }
        }
        description = """
            Automatically opens and closes an SSH reverse tunnel whenever you toggle
            <b>Run &rarr; Start Listening for PHP Debug Connections</b> in PHPStorm.<br><br>
            Configure the remote host, ports, and optional SSH key in
            <b>Settings &rarr; Tools &rarr; PHP Debug Tunnel</b>.
        """.trimIndent()
    }
}

kotlin {
    jvmToolchain(21)
}
