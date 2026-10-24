package skywa1ker404.flickdocket

import android.app.Application
import skywa1ker404.flickdocket.di.AppContainer

class FlickDocketApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
