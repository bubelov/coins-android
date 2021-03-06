package com.bubelov.coins.rates

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.bubelov.coins.R
import com.bubelov.coins.model.CurrencyPair
import kotlinx.android.synthetic.main.fragment_exchange_rates.*
import kotlinx.coroutines.flow.*
import org.koin.android.viewmodel.ext.android.viewModel

class ExchangeRatesFragment : Fragment() {

    private val model: ExchangeRatesViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exchange_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.apply {
            setNavigationOnClickListener { findNavController().popBackStack() }

            inflateMenu(R.menu.exchange_rates)

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.currency -> {
                        val items = CurrencyPair.values()
                        val itemTitles = items.map { it.toString() }.toTypedArray()

                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.currency)
                            .setItems(itemTitles) { _, index ->
                                model.setSelectedPair(items[index])
                            }
                            .show()

                        true
                    }
                    else -> false
                }
            }
        }

        ratesView.layoutManager = LinearLayoutManager(requireContext())

        model.selectedPair.onEach {
            toolbar.menu.findItem(R.id.currency).title = it.toString()
        }.launchIn(lifecycleScope)

        model.rows.onEach {
            ratesView.adapter = ExchangeRatesAdapter(it)
        }.launchIn(lifecycleScope)

        model.setSelectedPair(CurrencyPair.BTC_USD)
    }
}