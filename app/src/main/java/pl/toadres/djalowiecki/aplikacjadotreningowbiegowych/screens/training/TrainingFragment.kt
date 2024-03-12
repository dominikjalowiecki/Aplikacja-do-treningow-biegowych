package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.training

import android.Manifest
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.MainActivity
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.R
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.RunnerAppDatabase
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.databinding.FragmentTrainingBinding


class TrainingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentTrainingBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_training, container, false)

        val application = requireNotNull(this.activity).application
        val dataSource = RunnerAppDatabase.getInstance(application).trainingDao
        val arguments = TrainingFragmentArgs.fromBundle(requireArguments())
        val viewModelFactory = TrainingViewModelFactory(dataSource, arguments.trainingId)
        val trainingViewModel =
            ViewModelProvider(this, viewModelFactory)[TrainingViewModel::class.java]

        binding.trainingViewModel = trainingViewModel

        trainingViewModel.navigateToResult.observe(viewLifecycleOwner) { stoppedTraining ->
            stoppedTraining?.let {
                this.findNavController().navigate(
                    TrainingFragmentDirections.actionTrainingFragmentToResultFragment(it)
                )
                trainingViewModel.onResultNavigated()
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

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    val builder =
                        NotificationCompat.Builder(requireContext(), "training_notifications")
                    builder
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Notification title")
                        .setContentText("Notification text")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                    val notificationManager = getActivity()?.getSystemService(
                        NOTIFICATION_SERVICE
                    ) as NotificationManager

                    notificationManager.notify(
                        1, builder.build()
                    );
                } else {
                    // permission not granted
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

            } else {
                // repeat the permission or open app details
            }
        }

        return binding.root
    }

}