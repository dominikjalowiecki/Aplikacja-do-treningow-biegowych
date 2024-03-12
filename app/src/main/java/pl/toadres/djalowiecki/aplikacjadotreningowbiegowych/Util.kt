package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.Training
import java.text.SimpleDateFormat
import java.util.Calendar

@BindingAdapter("trainingString")
fun TextView.setTrainingString(item: Training?) {
    item?.let {
        text = item.trainingId.toString()
    }
}

@BindingAdapter("distanceString", "format")
fun TextView.setDistanceString(item: Training?, format: String) {
    item?.let {
        text = format.format(item.distance.toDouble() / 1000)
    }
}

@BindingAdapter("timeString", "format")
fun TextView.setTimeString(item: Training?, format: String) {
    item?.let {
        val trainingTimeMilli = item.endTimeMilli!! - item.startTimeMilli
        text = format.format(getTimeStringFromMilli(trainingTimeMilli))
    }
}

@BindingAdapter("paceString", "format")
fun TextView.setPaceString(item: Training?, format: String) {
    item?.let {
        val trainingTimeMilli = item.endTimeMilli!! - item.startTimeMilli
        var paceTimeMilli = 0L
        if (item.distance > 0) {
            paceTimeMilli = trainingTimeMilli / item.distance * 1000
        }
        text = format.format(getPaceStringFromMilli(paceTimeMilli))
    }
}

@BindingAdapter("speedString", "format")
fun TextView.setSpeedString(item: Training?, format: String) {
    item?.let {
        val trainingTimeMilli = item.endTimeMilli!! - item.startTimeMilli
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
    val formatter = SimpleDateFormat(dateFormat)
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

class TextItemViewHolder(val textView: TextView): RecyclerView.ViewHolder(textView)