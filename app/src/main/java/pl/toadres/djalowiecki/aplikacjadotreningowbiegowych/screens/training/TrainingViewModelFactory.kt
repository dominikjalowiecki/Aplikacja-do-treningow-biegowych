package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.TrainingDao

class TrainingViewModelFactory(
    private val dataSource: TrainingDao,
    private val trainingId: Long
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainingViewModel::class.java)) {
            return TrainingViewModel(dataSource, trainingId) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}