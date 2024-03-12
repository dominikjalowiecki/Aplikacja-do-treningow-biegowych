package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.TrainingDao

class HistoryViewModelFactory(
    private val dataSource: TrainingDao
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(dataSource) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}