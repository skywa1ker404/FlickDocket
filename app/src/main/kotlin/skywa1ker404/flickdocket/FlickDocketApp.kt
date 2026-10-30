package skywa1ker404.flickdocket

import android.app.Application
import skywa1ker404.flickdocket.di.AppContainer

class FlickDocketApp : Application() {
    override fun onCreate() { super.onCreate(); CrashReporter.install(this) }

    val container: AppContainer by lazy { AppContainer(this) }
}
