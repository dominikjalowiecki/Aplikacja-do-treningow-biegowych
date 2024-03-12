package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.screens.result

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.R
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database.Training
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.databinding.FragmentResultBinding
import pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.getTimeStringFromMilli

class ResultFragment : Fragment() {
    private lateinit var training: Training

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentResultBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_result, container, false
        )

        val arguments = ResultFragmentArgs.fromBundle(requireArguments())
        training = arguments.training

        binding.training = training

        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.result_menu, menu)
                if (getShareIntent().resolveActivity(requireActivity().packageManager) == null) {
                    menu.findItem(R.id.share)?.isVisible = false
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.share -> {
                        shareResult()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun shareResult() {
        startActivity(getShareIntent())
    }

    private fun getShareIntent(): Intent {
        val trainingTimeMilli = training.endTimeMilli!! - training.startTimeMilli
        return ShareCompat.IntentBuilder(requireActivity())
            .setText(
                getString(
                    R.string.share_text,
                    training.distance.toDouble().div(1000),
                    getTimeStringFromMilli(trainingTimeMilli)
                )
            )
            .setType("text/plain")
            .intent
    }
}