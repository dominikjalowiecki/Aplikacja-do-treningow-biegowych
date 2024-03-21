package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.Training
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.TrainingDao

class HistoryViewModel(private val database: TrainingDao) :
    ViewModel() {

    val trainings = database.getAllTrainings()

    val clearButtonVisible = trainings.map {
        it.isNotEmpty()
    }

    private val _showSnackbarEvent = MutableLiveData<Boolean>()

    val showSnackbarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent


    private val _navigateToTrainingResult = MutableLiveData<Training?>()

    val navigateToTrainingResult: LiveData<Training?>
        get() = _navigateToTrainingResult

    fun doneShowingSnackBar() {
        _showSnackbarEvent.value = false
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }

    fun onClear() {
        viewModelScope.launch {
            clear()
        }

        _showSnackbarEvent.value = true
    }

    fun onTrainingClicked(training: Training) {
        _navigateToTrainingResult.value = training
    }

    fun onTrainingResultNavigated() {
        _navigateToTrainingResult.value = null
    }
}