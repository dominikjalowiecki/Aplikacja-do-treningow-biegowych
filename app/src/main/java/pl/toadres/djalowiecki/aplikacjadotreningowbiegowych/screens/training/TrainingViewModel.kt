package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.training

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.Training
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.TrainingDao

class TrainingViewModel(private val database: TrainingDao, private val trainingId: Long) :
    ViewModel() {

    private val training = MediatorLiveData<Training>()

    fun getTraining() = training

    init {
        training.addSource(database.getTrainingWithId(trainingId), training::setValue)
    }

    private suspend fun update(training: Training) {
        withContext(Dispatchers.IO) {
            database.update(training)
        }
    }

    fun onStopTraining() {
        viewModelScope.launch {
            val stoppedTraining = training.value ?: return@launch
            stoppedTraining.endTimeMilli = System.currentTimeMillis()

            update(stoppedTraining)
        }
    }

}