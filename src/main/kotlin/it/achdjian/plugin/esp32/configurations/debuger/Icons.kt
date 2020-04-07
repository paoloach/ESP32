package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.ide.ui.ProductIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.NotNullLazyValue

val DEBUG_ICON =NotNullLazyValue.createConstantValue(IconLoader.findIcon("/images/icon.png") ?: ProductIcons.getInstance().productIcon)