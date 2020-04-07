package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.execution.remote.RemoteGDBLauncher
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.debugger.remote.CidrRemoteDebugParameters

class ESP32DebugLauncher(project: Project, myDebuggerToolchain: CPPToolchains.Toolchain, val openOcdConfiguration: ESP32DebugRunConfiguration) :
    RemoteGDBLauncher(project, myDebuggerToolchain, CidrRemoteDebugParameters()){


}