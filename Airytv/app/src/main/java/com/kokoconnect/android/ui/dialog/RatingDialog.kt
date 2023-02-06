package com.kokoconnect.android.ui.dialog

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.DialogRatingBinding
import com.kokoconnect.android.databinding.DialogRatingGooglePlayBinding
import com.kokoconnect.android.model.event.AmsEventsFacade
import com.kokoconnect.android.model.notification.RatingManager

class RatingDialog(
    val ratingManager: RatingManager?,
    val events: AmsEventsFacade?
) : DialogFragment() {
    companion object {
        const val TAG = "RatingDialog"
    }

    constructor() : this(null, null)

    private var binding: DialogRatingBinding? = null
    private var googlePlayDialogOpened = false

    override fun getView(): View? {
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val binding = DialogRatingBinding.inflate(inflater)
        this.binding = binding

        val dialogBuilder = AlertDialog.Builder(
            requireActivity(),
            R.style.RectCornersDialog
        ).setView(binding.root)
        val dialog = dialogBuilder.create()

        binding.btnSend.setOnClickListener {
            googlePlayDialogOpened = true
            RatingGooglePlayDialog(ratingManager, events).show(
                parentFragmentManager,
                RatingGooglePlayDialog.TAG
            )
            dismiss()
        }
        binding.btnLater.setOnClickListener {
            dismiss()
        }
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        events?.sendRatingEventStars()
        return dialog
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (googlePlayDialogOpened) {

        } else {
            ratingManager?.setCurrentDateForLastCheck()
            ratingManager?.setFirstDialog(false)
            ratingManager?.clearSecondsOfUse()
            events?.sendRatingEventStarsCancel()
        }
    }
}

class RatingGooglePlayDialog(
    val ratingManager: RatingManager?,
    val events: AmsEventsFacade?
): DialogFragment() {
    companion object {
        const val TAG = "RatingGooglePlayDialog"
    }

    var binding: DialogRatingGooglePlayBinding? = null
    private var googlePlayOpened = false

    constructor() : this(null, null)

    override fun getView(): View? {
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val binding = DialogRatingGooglePlayBinding.inflate(inflater)
        this.binding = binding

        val dialogBuilder = AlertDialog.Builder(
            requireActivity(),
            R.style.RectCornersDialog
        ).setView(binding.root)
        val dialog = dialogBuilder.create()

        binding.btnOpenGooglePlay.setOnClickListener {
            googlePlayOpened = true
            openPlayStore(context)
            dismiss()
        }
        binding.ivClose.setOnClickListener {
            dismiss()
        }
        return dialog
    }

    private fun openPlayStore(context: Context?) {
        context ?: return
        val uri = Uri.parse("market://details?id=" + context.packageName)
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(
            (Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        )
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + context.packageName)
                )
            )
        }
    }

    override fun onPause() {
        super.onPause()
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (googlePlayOpened) {
            events?.sendRatingEventGooglePlay()
        } else {
            ratingManager?.setCurrentDateForLastCheck()
            ratingManager?.setFirstDialog(false)
            ratingManager?.clearSecondsOfUse()
            events?.sendRatingEventGooglePlayCancel()
        }
    }
}