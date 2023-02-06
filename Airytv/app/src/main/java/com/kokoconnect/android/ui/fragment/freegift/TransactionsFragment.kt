package com.kokoconnect.android.ui.fragment.freegift

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kokoconnect.android.adapter.TransactionsAdapter
import com.kokoconnect.android.databinding.FragmentTransactionsBinding
import com.kokoconnect.android.di.Injectable
import com.kokoconnect.android.di.ViewModelFactory
import com.kokoconnect.android.model.ui.Screen
import com.kokoconnect.android.model.ui.ScreenType
import com.kokoconnect.android.model.giveaways.TransactionItem
import com.kokoconnect.android.model.giveaways.TransactionItemData
import com.kokoconnect.android.model.giveaways.TransactionItemHeader
import com.kokoconnect.android.vm.NavigationViewModel
import com.kokoconnect.android.vm.freegift.GiveawaysViewModel
import com.kokoconnect.android.vm.profile.ProfileViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class TransactionsFragment: Fragment(), Injectable {
    companion object {
        val screen = Screen().apply {
            this.name = ScreenType.TRANSACTIONS.defaultName
            this.type = ScreenType.TRANSACTIONS
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    var adapter: TransactionsAdapter = TransactionsAdapter()
    var binding: FragmentTransactionsBinding? = null


    private val profileViewModel: ProfileViewModel by activityViewModels { viewModelFactory }
    private val giveawaysViewModel: GiveawaysViewModel by activityViewModels { viewModelFactory }
    private val navigationViewModel: NavigationViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        profileViewModel.profileLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                showTransactions()
            }
        })
        giveawaysViewModel.transactions.observe(viewLifecycleOwner, Observer {
            it?.let {
                showTransactions()
            }
        })

        updateData()
    }

    private fun setupViews() {
        binding?.rvTransactions?.adapter = adapter
        binding?.rvTransactions?.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    }

    private fun showTransactions() {
        val profile = profileViewModel.getProfile()
        val transactions = giveawaysViewModel.getTransactions()
        val transactionItems = mutableListOf<TransactionItem>()
        profile?.let{
            transactionItems.add(TransactionItemHeader(it))
        }
        transactions?.forEach {
            transactionItems.add(TransactionItemData(it))
        }
        adapter.items = transactionItems
    }

    private fun updateData() {
        lifecycleScope.launch {
            profileViewModel.requestProfile()
            giveawaysViewModel.requestTransactions()
        }
    }


}