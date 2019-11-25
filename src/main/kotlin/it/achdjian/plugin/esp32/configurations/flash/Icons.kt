package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.ide.ui.ProductIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.NotNullLazyValue
import javax.swing.Icon

object ICON_FLASH : NotNullLazyValue<Icon>() {
    override fun compute() = IconLoader.findIcon("/images/icon.png") ?: ProductIcons.getInstance().productIcon
}