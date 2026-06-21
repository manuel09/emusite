package com.emusite.app

import android.app.Application
import com.emusite.app.data.EmusiteAppHolder
import com.emusite.app.plugin.PluginManager
import com.emusite.app.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EmusiteApp : Application() {

    lateinit var pluginManager: PluginManager
        private set
    lateinit var mediaRepository: MediaRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        EmusiteAppHolder.app = this

        pluginManager = PluginManager(this)
        mediaRepository = MediaRepository(pluginManager)

        applicationScope.launch {
            pluginManager.initialize()
        }
    }
}
