package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.training

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.MainActivity
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.R
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.RunnerAppDatabase
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.databinding.FragmentTrainingBinding
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.services.TrainingService


class TrainingFragment : Fragment() {

    private lateinit var trainingService: Intent
    private val handler = Handler(Looper.getMainLooper())
    private val delay = 1000L
    private lateinit var runnable: Runnable
    private lateinit var binding: FragmentTrainingBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_training, container, false)

        val application = requireNotNull(this.activity).application
        val dataSource = RunnerAppDatabase.getInstance(application).trainingDao
        val arguments = TrainingFragmentArgs.fromBundle(requireArguments())
        val trainingId = arguments.trainingId
        val viewModelFactory = TrainingViewModelFactory(dataSource, trainingId)
        val trainingViewModel =
            ViewModelProvider(this, viewModelFactory)[TrainingViewModel::class.java]

        binding.trainingViewModel = trainingViewModel

        trainingViewModel.getTraining().observe(viewLifecycleOwner) { training ->
            training?.let {
                if (training.endTimeMilli != null) {
                    this.findNavController().navigate(
                        TrainingFragmentDirections.actionTrainingFragmentToResultFragment(training)
                    )
                }
            }
        }

        val activity = requireActivity()
        activity.onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    trainingViewModel.onStopTraining()
                }
            })

        (requireActivity() as MainActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        binding.lifecycleOwner = this

        trainingService = Intent(context, TrainingService::class.java)
        trainingService.putExtra("trainingId", trainingId)
        requireActivity().startForegroundService(trainingService)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        runnable = Runnable {
            binding.invalidateAll()
            handler.postDelayed(runnable, delay)
        }

        handler.postDelayed(runnable, delay)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().stopService(trainingService)
    }
}