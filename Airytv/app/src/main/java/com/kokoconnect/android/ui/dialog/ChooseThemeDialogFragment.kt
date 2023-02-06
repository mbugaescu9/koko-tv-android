package com.kokoconnect.android.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.kokoconnect.android.R
import com.kokoconnect.android.model.settings.UiTheme
import com.kokoconnect.android.databinding.DialogFragmentChooseThemeBinding

class ChooseThemeDialogFragment : DialogFragment() {
    private var listener: Listener? = null
    var binding: DialogFragmentChooseThemeBinding? = null

    override fun getView(): View? {
        return binding?.root
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogFragmentChooseThemeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.buttonNew?.setOnClickListener {
            listener?.onChooseTheme(UiTheme.UI_THEME_NEW)
            dismiss()
        }

        binding?.buttonClassic?.setOnClickListener {
            listener?.onChooseTheme(UiTheme.UI_THEME_CLASSIC)
            dismiss()
        }
        binding?.ivClose?.setOnClickListener {
            dismiss()
        }
        binding?.root?.setOnClickListener {
            dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    interface Listener {
        fun onChooseTheme(theme: UiTheme)
    }
}