package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.title

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.TrainingDao

class TitleViewModelFactory(
    private val dataSource: TrainingDao
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TitleViewModel::class.java)) {
            return TitleViewModel(dataSource) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}