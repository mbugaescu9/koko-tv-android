package com.kokoconnect.android.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kokoconnect.android.databinding.DialogFragmentPickImageBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PickImageDialog: BottomSheetDialogFragment() {
    companion object {
        const val TAG = "PickImageDialog"
    }
    var listener: Listener? = null
    private var binding: DialogFragmentPickImageBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogFragmentPickImageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding?.llCamera?.setOnClickListener {
            listener?.pickImage(ImageSource.CAMERA)
            dismiss()
        }
        binding?.llGallery?.setOnClickListener {
            listener?.pickImage(ImageSource.GALLERY)
            dismiss()
        }
    }

    enum class ImageSource() {
        CAMERA,
        GALLERY()
    }

    interface Listener {
        fun pickImage(imageSource: ImageSource)
    }
}