package com.noobfree.android

import android.app.Application
import com.noobfree.android.data.DeviceRepository
import com.noobfree.android.data.ShizukuRepository
import com.noobfree.android.system.AnimationManager
import com.noobfree.android.system.DebugManager
import com.noobfree.android.system.DeveloperManager
import com.noobfree.android.system.DeviceInfoManager
import com.noobfree.android.system.RefreshRateManager

class NoobFreeApplication : Application() {

    lateinit var shizukuRepository: ShizukuRepository
        private set

    lateinit var deviceRepository: DeviceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        shizukuRepository = ShizukuRepository(applicationContext)
        shizukuRepository.registerListeners()

        val developerManager = DeveloperManager(contentResolver, shizukuRepository)
        val animationManager = AnimationManager(contentResolver, shizukuRepository)
        val debugManager = DebugManager(contentResolver, shizukuRepository)
        val refreshRateManager = RefreshRateManager(applicationContext)
        val deviceInfoManager = DeviceInfoManager(applicationContext)

        deviceRepository = DeviceRepository(
            context = applicationContext,
            developerManager = developerManager,
            animationManager = animationManager,
            debugManager = debugManager,
            refreshRateManager = refreshRateManager,
            deviceInfoManager = deviceInfoManager
        )
    }

    override fun onTerminate() {
        shizukuRepository.unregisterListeners()
        super.onTerminate()
    }
}
