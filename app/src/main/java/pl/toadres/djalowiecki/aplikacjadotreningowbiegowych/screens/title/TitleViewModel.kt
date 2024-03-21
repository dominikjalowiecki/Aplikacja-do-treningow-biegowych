package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.title

import android.text.TextWatcher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.SimpleTextWatcher
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.Training
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.TrainingDao

class TitleViewModel(
    private val database: TrainingDao
) : ViewModel() {

    private val _permissionsGranted = MutableLiveData(false)

    val permissionsGranted: LiveData<Boolean>
        get() = _permissionsGranted

    private val _navigateToTraining = MutableLiveData<Training?>()
    val navigateToTraining: LiveData<Training?>
        get() = _navigateToTraining

    val trainingAlreadyStarted = _navigateToTraining.map {
        it != null
    }

    private val _targetTime = MutableLiveData<Int?>()
    private val _targetDistance = MutableLiveData<Int?>()

    fun getTargetTimeWatcher(): TextWatcher {
        val simpleTextWatcher = SimpleTextWatcher()
        simpleTextWatcher.onTextChanged {
            var targetTime = it.toIntOrNull()
            _targetTime.value = targetTime
        }
        return simpleTextWatcher
    }

    fun getTargetDistanceWatcher(): TextWatcher {
        val simpleTextWatcher = SimpleTextWatcher()
        simpleTextWatcher.onTextChanged {
            var targetDistance = it.toIntOrNull()
            _targetDistance.value = targetDistance
        }
        return simpleTextWatcher
    }

    val targetTimeString = _targetTime.map {
        it?.toString() ?: ""
    }

    val targetDistanceString = _targetDistance.map {
        it?.toString() ?: ""
    }

    init {
        initializeTraining()
    }

    private fun initializeTraining() {
        viewModelScope.launch {
            val training = getLatestTrainingFromDatabase()
            if (_permissionsGranted.value == true) {
                _navigateToTraining.value = training
            } else {
                if (training != null) {
                    training.endTimeMilli = System.currentTimeMillis()
                    update(training)
                }
            }
        }
    }


    private suspend fun getLatestTrainingFromDatabase(): Training? {
        return withContext(Dispatchers.IO) {
            var training = database.getLatestTraining()
            if (training?.endTimeMilli != null) {
                training = null
            }

            return@withContext training
        }
    }


    private suspend fun insert(training: Training) {
        withContext(Dispatchers.IO) {
            database.insert(training)
        }
    }

    private suspend fun update(training: Training) {
        withContext(Dispatchers.IO) {
            database.update(training)
        }
    }

    fun onStartTraining() {
        viewModelScope.launch {
            val startedTraining = Training()
            if ((_targetTime.value ?: 0) > 0) {
                startedTraining.targetTimeMilli = _targetTime.value?.toLong()?.times(60000)
            }

            if ((_targetDistance.value ?: 0) > 0) {
                startedTraining.targetDistance = _targetDistance.value?.times(1000)
            }

            insert(startedTraining)

            _navigateToTraining.value = getLatestTrainingFromDatabase()
        }
    }

    fun onTrainingNavigated() {
        _targetTime.value = null
        _targetDistance.value = null
        _navigateToTraining.value = null
    }

    fun onPermissionsGranted() {
        _permissionsGranted.value = true
    }
}