package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych

import android.app.Application
import timber.log.Timber

class RunnerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }

}