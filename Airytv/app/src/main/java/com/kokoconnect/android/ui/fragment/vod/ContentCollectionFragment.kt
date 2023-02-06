package com.kokoconnect.android.ui.fragment.vod

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.kokoconnect.android.databinding.FragmentContentCollectionBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.vm.vod.VodContentViewModel
import com.kokoconnect.android.vm.NavigationViewModel
import javax.inject.Inject

class ContentCollectionFragment : Fragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.COLLECTION.defaultName
            this.type = ScreenType.COLLECTION
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private var binding: FragmentContentCollectionBinding? = null

    private val contentViewModel: VodContentViewModel by activityViewModels { viewModelFactory }
    private val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContentCollectionBinding.inflate(inflater)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        contentViewModel.needShowCollection.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                contentViewModel.currentCollection?.let { collection ->
                    setPrimaryTitle(collection.name)
                    navigationViewModel.setCurrentScreen(screen, this)
                }
            }
        })
    }

    private fun setupViews() {
        binding?.btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setPrimaryTitle(title: String?) {
        binding?.tvTitlePrimary?.setText(title ?: "")
        binding?.tvTitlePrimary?.visibility = if (title == null || title.isEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        navigationViewModel.setCurrentScreen(screen, this)
        setActionBarExpanded(true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun setActionBarExpanded(isExpanded: Boolean, animated: Boolean = false) {
        binding?.appbar?.setExpanded(isExpanded, animated)
    }


    fun lockUi() {
        binding?.lockUiLayout?.visibility = View.VISIBLE
        binding?.lockUiLayout?.setOnClickListener {
            // ignore
        }
    }

    fun unlockUi() {
        binding?.lockUiLayout?.visibility = View.GONE
    }
}