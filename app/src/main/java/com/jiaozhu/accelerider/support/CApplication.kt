package com.jiaozhu.accelerider.support

import android.app.Application
import com.jiaozhu.accelerider.commonTools.CrashHandler
import com.jiaozhu.accelerider.commonTools.Log
import java.io.File

/**
 * Created by apple on 15/10/30.
 */
class CApplication : Application() {

    override fun onCreate() {
        //TODO 在此初始化所有单例,注意初始化顺序
        super.onCreate()
        getExternalFilesDir(null)?.let {
            CrashHandler.init(this, it.path + "/crash.log")
        }
        UserInfo.context = this
        Constants(this)
        Constants.isDebug = Tools.isDebug(this)
        Log.setLevel(if (Constants.isDebug) Log.VERBOSE else Log.ASSERT)
    }


    private fun initPath(vararg paths: String) {
        var file: File
        for (str in paths) {
            file = File(str)
            if (!file.exists()) {
                file.mkdirs()

            }
        }
    }

}