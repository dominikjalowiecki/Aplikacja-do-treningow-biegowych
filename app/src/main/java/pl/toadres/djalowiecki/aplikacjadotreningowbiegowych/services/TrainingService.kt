package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.location.Location.distanceBetween
import androidx.navigation.NavDeepLinkBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.CHANNEL_ID
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.LOCATION_INTERVAL
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.MainActivity
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.R
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.RunnerAppDatabase
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.Training
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.TrainingDao
import timber.log.Timber
import kotlin.properties.Delegates

class TrainingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: TrainingDao
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var notificationManager: NotificationManager
    private var trainingId by Delegates.notNull<Long>()
    private var training: Training? = null
    private var lastLocation: Location? = null
    private var notificationDistance = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        trainingId = intent?.getLongExtra("trainingId", 0) ?: 0
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                training = database.get(trainingId)
            }
        }
        try {
            getLocation()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    1,
                    getNotification(
                        R.string.training_has_been_started,
                        R.string.training_in_progress,
                        autoCancel = false,
                        ongoing = true
                    ),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(
                    1,
                    getNotification(
                        R.string.training_has_been_started,
                        R.string.training_in_progress,
                        autoCancel = false,
                        ongoing = true
                    )
                )
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                Timber.e("Foreground service start not allowed")
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        database = RunnerAppDatabase.getInstance(application).trainingDao
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (lastLocation != null) {
                    val distance = FloatArray(1)
                    distanceBetween(
                        lastLocation!!.latitude,
                        lastLocation!!.longitude,
                        location.latitude,
                        location.longitude,
                        distance
                    )

                    if(distance[0] > 0) {
                        serviceScope.launch {
                            updateTrainingDistance(distance[0].toInt())
                        }
                    }

                    if(training != null) {
                        Timber.i(training!!.targetTimeMilli.toString())
                        if(training!!.targetTimeMilli != null && (System.currentTimeMillis() - training!!.startTimeMilli) >= training!!.targetTimeMilli!!) {
                            serviceScope.launch {
                                finishTraining()
                            }
                            notify(R.string.training_finished, R.string.check_training_result_in_history)
                        }
                    }

//                    notificationDistance += distance[0].toInt()
//                    if (notificationDistance >= 1000) {
//                        notificationDistance = 0
//                        if (training != null) {
//                            // TODO: Notification every 1 km or 15 min
//                            // TODO: End training and notification
//                            if (training!!.targetTimeMilli != null) {
//                                if (training!!.targetDistance != null) {
//                                    Timber.i("Twoje aktualne tempo to x m:s/km, a wymagane do ukończenia to y m:s/km. Przyśpiesz!")
//                                    Timber.i("Twoje tempo biegu jest dobre, tak trzymaj!")
//                                } else {
//                                    Timber.i("Pozostało x min treningu")
//                                }
//                            } else if (training!!.targetDistance != null) {
//                                Timber.i("Pozostało x km treningu")
//                            } else {
//                                Timber.i("Minęło x min treningu")
//                            }
//                        }
//                    }
                }
                lastLocation = location
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }

    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL,
                0f,
                locationListener,
                Looper.getMainLooper()
            )
        }
    }

    private fun notify(contentTitle: Int, contentText: Int) {
        notificationManager.notify(2, getNotification(contentTitle, contentText, true,false))
    }

    private fun getNotification(contentTitle: Int, contentText: Int, autoCancel: Boolean, ongoing: Boolean): Notification {
//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
//        )
        val pendingIntent = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.main_navigation)
            .setDestination(R.id.historyFragment)
            .createPendingIntent()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(contentTitle))
            .setContentText(getString(contentText))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(autoCancel)
            .setOngoing(ongoing)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }

    private suspend fun updateTrainingDistance(distance: Int) {
        withContext(Dispatchers.IO) {
            database.updateTrainingDistance(trainingId, distance)
        }
    }

    private suspend fun finishTraining() {
        withContext(Dispatchers.IO) {
            database.finishTraining(trainingId, System.currentTimeMillis())
        }
    }
}