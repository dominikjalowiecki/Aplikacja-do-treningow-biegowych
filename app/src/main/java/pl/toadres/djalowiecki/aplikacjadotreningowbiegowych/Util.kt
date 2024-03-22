package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.databinding.BindingAdapter
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.Training
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val CHANNEL_ID = "training_notifications"
const val LOCATION_INTERVAL = 5000L
const val NOTIFICATION_DISTANCE = 1000 // 1 km
const val NOTIFICATION_TIME = 900000 // 15 min
const val MAX_LOCATION_ACCURACY = 30

@BindingAdapter("distanceString", "format")
fun TextView.setDistanceString(item: Training?, format: String) {
    item?.let {
        text = format.format(getDistance(item.distance))
    }
}

@BindingAdapter("timeString", "format")
fun TextView.setTimeString(item: Training?, format: String) {
    item?.let {
        val trainingTimeMilli =
            (item.endTimeMilli ?: System.currentTimeMillis()) - item.startTimeMilli
        text = format.format(getTimeStringFromMilli(trainingTimeMilli))
    }
}

@BindingAdapter("paceString", "format")
fun TextView.setPaceString(item: Training?, format: String) {
    item?.let {
        val trainingTimeMilli =
            (item.endTimeMilli ?: System.currentTimeMillis()) - item.startTimeMilli
        var paceTimeMilli = 0L
        if (item.distance > 0) {
            paceTimeMilli = getPace(trainingTimeMilli, item.distance)
        }
        text = format.format(getPaceStringFromMilli(paceTimeMilli))
    }
}

@BindingAdapter("speedString", "format")
fun TextView.setSpeedString(item: Training?, format: String) {
    item?.let {
        val trainingTimeMilli =
            (item.endTimeMilli ?: System.currentTimeMillis()) - item.startTimeMilli
        var speed = 0.0
        if (item.distance > 0) {
            speed = item.distance.toDouble() / 1000 / trainingTimeMilli * 3600000
        }
        text = format.format(speed)
    }
}

@BindingAdapter("dateString", "format")
fun TextView.setDateString(item: Training?, format: String) {
    item?.let {
        text = format.format(getDate(item.startTimeMilli, "dd.MM.yyyy HH:mm:ss"))
    }
}

fun getDistance(distance: Int): Double {
    return distance.toDouble() / 1000
}

fun getPace(time: Long, distance: Int): Long {
    return time / distance * 1000
}

fun getPaceStringFromMilli(timeMilli: Long): String {
    var timeSeconds = timeMilli / 1000
    var timeMinutes = timeSeconds / 60

    timeSeconds %= 60

    return "%02d:%02d".format(timeMinutes, timeSeconds)
}

fun getTimeStringFromMilli(timeMilli: Long): String {
    var timeSeconds = timeMilli / 1000
    var timeMinutes = timeSeconds / 60
    var timeHours = timeMinutes / 60

    timeSeconds %= 60
    timeMinutes %= 60

    return "%02d:%02d:%02d".format(timeHours, timeMinutes, timeSeconds)
}

fun getDate(timeMilli: Long, dateFormat: String): String {
    val formatter = SimpleDateFormat(dateFormat, Locale.US)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMilli

    return formatter.format(calendar.time)
}

class SimpleTextWatcher : TextWatcher {
    private var _onTextChanged: ((String) -> Unit)? = null

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun afterTextChanged(s: Editable?) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        _onTextChanged?.invoke(s.toString())
    }

    fun onTextChanged(listener: (String) -> Unit) {
        _onTextChanged = listener
    }
}