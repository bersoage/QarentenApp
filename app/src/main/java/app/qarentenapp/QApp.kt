package app.qarentenapp

import android.app.Application
import app.qarentenapp.core.Repo
import app.qarentenapp.core.UpdateWatcher

class QApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
        UpdateWatcher.schedule(this)
    }
}
