package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.title

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.R
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.RunnerAppDatabase
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.databinding.FragmentTitleBinding
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.CHANNEL_ID

class TitleFragment : Fragment() {

    private lateinit var titleViewModel: TitleViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentTitleBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_title, container, false
        )

        val application = requireNotNull(this.activity).application
        val dataSource = RunnerAppDatabase.getInstance(application).trainingDao
        val viewModelFactory = TitleViewModelFactory(dataSource)
        titleViewModel = ViewModelProvider(this, viewModelFactory)[TitleViewModel::class.java]

        binding.titleViewModel = titleViewModel

        titleViewModel.navigateToTraining.observe(viewLifecycleOwner) { startedTraining ->
            startedTraining?.let {
                this.findNavController().navigate(
                    TitleFragmentDirections.actionTitleFragmentToTrainingFragment(it.trainingId)
                )
                titleViewModel.onTrainingNavigated()
            }
        }

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(permission: String, onGranted: () -> Unit, onDenied: () -> Unit) {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                when {
                    isGranted -> {
                        onGranted()
                    }

                    else -> {
                        onDenied()
                    }
                }
            }
        requestPermissionLauncher.launch(permission)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                createNotificationChannel()
            } else {
                askForPostNotificationsPermission()
            }
        }

        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                checkBatteryOptimization()
                titleViewModel.onPermissionsGranted()
            } else {
                askForBackgroundLocationPermission()
            }
        } else {
            askForLocationPermission()
        }
    }

    private fun askForPostNotificationsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, {}, {})
        }
    }

    private fun askForLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, {
                if (checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    checkBatteryOptimization()
                    titleViewModel.onPermissionsGranted()
                } else {
                    askForBackgroundLocationPermission();
                }
            }, {
                permissionsRequired()
            })
        } else {
            permissionsRequired()
        }
    }

    private fun askForBackgroundLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, {
                    checkBatteryOptimization()
                    titleViewModel.onPermissionsGranted()
                }, {
                    permissionsRequired()
                })
            } else {
                checkBatteryOptimization()
                titleViewModel.onPermissionsGranted()
            }
        } else {
            permissionsRequired()
        }
    }

    private fun permissionsRequired() {
        Toast.makeText(requireActivity(), R.string.permissions_required, Toast.LENGTH_LONG)
            .show()
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireActivity().packageName, "TitleFragment")
        )
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.overflow_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return NavigationUI.onNavDestinationSelected(
                    menuItem,
                    requireView().findNavController()
                )
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun checkBatteryOptimization() {
        val powerManager = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(requireActivity().packageName)) {
            Toast.makeText(
                requireActivity(),
                R.string.ignore_battery_optimization,
                Toast.LENGTH_LONG
            )
                .show()
            try {
                val intent = Intent(
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                    Uri.fromParts("package", requireActivity().packageName, "TitleFragment")
                )
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", requireActivity().packageName, "TitleFragment")
                )
                startActivity(intent)
            }
        }
    }
}