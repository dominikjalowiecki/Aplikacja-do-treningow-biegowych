package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.services

import android.app.Notification
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
import androidx.navigation.NavDeepLinkBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.CHANNEL_ID
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.LOCATION_INTERVAL
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.NOTIFICATION_DISTANCE
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.NOTIFICATION_TIME
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.MAX_LOCATION_ACCURACY
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.MainActivity
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.R
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.RunnerAppDatabase
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.Training
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.TrainingDao
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.getDistance
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.getPace
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.getTimeStringFromMilli
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
    private var totalDistance = 0
    private var notificationDistance = 0
    private var notificationTime = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        trainingId = intent?.getLongExtra("trainingId", 0) ?: 0
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                training = database.get(trainingId)
            }
        }

        getLocation()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                getNotification(
                    getString(R.string.training_has_been_started),
                    getString(R.string.training_in_progress),
                    autoCancel = false,
                    ongoing = true,
                    null
                ),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(
                1,
                getNotification(
                    getString(R.string.training_has_been_started),
                    getString(R.string.training_in_progress),
                    autoCancel = false,
                    ongoing = true,
                    null
                )
            )
        }

        return START_REDELIVER_INTENT
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
                if (notificationTime == 0L) {
                    notificationTime = System.currentTimeMillis()
                }

                if (location.hasAccuracy() && location.accuracy < MAX_LOCATION_ACCURACY) {
                    if (lastLocation != null) {
                        val distanceArray = FloatArray(1)
                        Location.distanceBetween(
                            lastLocation!!.latitude,
                            lastLocation!!.longitude,
                            location.latitude,
                            location.longitude,
                            distanceArray
                        )
                        val distance = distanceArray[0].toInt()

                        if (distance > 0) {
                            serviceScope.launch {
                                updateTrainingDistance(distance)
                            }
                            totalDistance += distance
                            notificationDistance += distance
                        }

                        if (training != null) {
                            val currentTimeMilli =
                                System.currentTimeMillis() - training!!.startTimeMilli
                            val targetTimeMilli = training!!.targetTimeMilli
                            val targetDistance = training!!.targetDistance

                            if ((targetTimeMilli != null && currentTimeMilli >= targetTimeMilli)
                                || (targetDistance != null && totalDistance >= targetDistance)
                            ) {
                                serviceScope.launch {
                                    finishTraining()
                                }
                                notify(
                                    getString(R.string.training_finished),
                                    getString(R.string.check_training_result_in_history),
                                    R.id.historyFragment
                                )
                                stopForeground(STOP_FOREGROUND_REMOVE)
                                stopSelf()
                                return
                            }

                            if (notificationDistance >= NOTIFICATION_DISTANCE) {
                                notificationDistance = 0
                                if (targetDistance != null && targetTimeMilli == null) {
                                    notify(
                                        getString(R.string.training_informations),
                                        getString(
                                            R.string.distance_of_training_remaining,
                                            getDistance(targetDistance - totalDistance)
                                        ),
                                        null
                                    )
                                }
                            }

                            if ((System.currentTimeMillis() - notificationTime) >= NOTIFICATION_TIME) {
                                notificationTime = System.currentTimeMillis()
                                if (targetTimeMilli != null) {
                                    if (targetDistance != null) {
                                        var currentPace = 0L
                                        if (totalDistance > 0) {
                                            currentPace = getPace(currentTimeMilli, totalDistance)
                                        }
                                        val targetPace = getPace(targetTimeMilli, targetDistance)
                                        if (currentPace <= targetPace) {
                                            notify(
                                                getString(R.string.training_informations),
                                                getString(R.string.your_running_pace_is_good),
                                                null,
                                            )
                                        } else {
                                            notify(
                                                getString(R.string.training_informations),
                                                getString(R.string.your_running_pace_is_too_slow),
                                                null,
                                            )
                                        }
                                    } else {
                                        notify(
                                            getString(R.string.training_informations),
                                            getString(
                                                R.string.time_of_training_remaining,
                                                getTimeStringFromMilli(targetTimeMilli - currentTimeMilli)
                                            ),
                                            null
                                        )
                                    }
                                } else if (targetDistance == null) {
                                    notify(
                                        getString(R.string.training_informations),
                                        getString(
                                            R.string.time_of_training_have_passed,
                                            getTimeStringFromMilli(currentTimeMilli)
                                        ),
                                        null
                                    )
                                }
                            }
                        }
                    }
                    lastLocation = location
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

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

    private fun notify(contentTitle: String, contentText: String, destination: Int?) {
        notificationManager.notify(
            2,
            getNotification(
                contentTitle,
                contentText,
                autoCancel = true,
                ongoing = false,
                destination
            )
        )
    }

    private fun getNotification(
        contentTitle: String,
        contentText: String,
        autoCancel: Boolean,
        ongoing: Boolean,
        destination: Int?
    ): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(autoCancel)
            .setOngoing(ongoing)

        if (destination != null) {
            val pendingIntent = NavDeepLinkBuilder(this)
                .setComponentName(MainActivity::class.java)
                .setGraph(R.navigation.main_navigation)
                .setDestination(destination)
                .createPendingIntent()
            builder.setContentIntent(pendingIntent)
        }

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