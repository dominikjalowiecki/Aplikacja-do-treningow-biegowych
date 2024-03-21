package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.R
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.RunnerAppDatabase
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentHistoryBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_history, container, false)

        val application = requireNotNull(this.activity).application
        val dataSource = RunnerAppDatabase.getInstance(application).trainingDao
        val viewModelFactory = HistoryViewModelFactory(dataSource)
        val historyViewModel =
            ViewModelProvider(this, viewModelFactory)[HistoryViewModel::class.java]

        binding.historyViewModel = historyViewModel

        historyViewModel.showSnackbarEvent.observe(viewLifecycleOwner) {
            if (it == true) {
                Snackbar.make(
                    requireActivity().findViewById(android.R.id.content),
                    getString(R.string.history_cleared_message),
                    Snackbar.LENGTH_SHORT
                ).show()

                historyViewModel.doneShowingSnackBar()
            }
        }

        val adapter = TrainingAdapter(TrainingListener { training ->
            historyViewModel.onTrainingClicked(training)
        })

        binding.trainingList.adapter = adapter

        historyViewModel.trainings.observe(viewLifecycleOwner) {
            it?.let {
                adapter.addHeaderAndSubmitList(it)
            }
        }

        historyViewModel.navigateToTrainingResult.observe(viewLifecycleOwner) {
            it?.let {
                this.findNavController()
                    .navigate(HistoryFragmentDirections.actionHistoryFragmentToResultFragment(it))
                historyViewModel.onTrainingResultNavigated()
            }
        }

        binding.lifecycleOwner = this

        return binding.root
    }

}