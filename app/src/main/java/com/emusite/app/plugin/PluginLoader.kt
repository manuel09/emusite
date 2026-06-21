package com.emusite.app.plugin

import android.content.Context
import com.emusite.api.Plugin
import dalvik.system.DexClassLoader
import java.io.File

class PluginLoader(private val context: Context) {

    fun loadPlugin(apkPath: String): Plugin {
        val pluginDir = context.getDir("plugin_dex", Context.MODE_PRIVATE)
        val optimizedDir = File(pluginDir, "odex").also { it.mkdirs() }
        val libDir = File(pluginDir, "lib").also { it.mkdirs() }

        val file = File(apkPath)
        check(file.exists()) { "File not found: ${file.absolutePath}" }

        val packageInfo = context.packageManager.getPackageArchiveInfo(apkPath, 0)
        checkNotNull(packageInfo) { "getPackageArchiveInfo returned null (file size=${file.length()})" }

        val packageName = packageInfo.packageName
        val className = "$packageName.PluginImpl"

        val classLoader = DexClassLoader(
            apkPath,
            optimizedDir.absolutePath,
            libDir.absolutePath,
            context.classLoader
        )

        val clazz = classLoader.loadClass(className)
        return clazz.getDeclaredConstructor().newInstance() as Plugin
    }

    fun loadPluginFromClass(clazz: Class<*>): Plugin {
        return clazz.getDeclaredConstructor().newInstance() as Plugin
    }
}
