package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.title

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.R
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.RunnerAppDatabase
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.databinding.FragmentTitleBinding
import timber.log.Timber

class TitleFragment : Fragment() {

    private lateinit var titleViewModel: TitleViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

}