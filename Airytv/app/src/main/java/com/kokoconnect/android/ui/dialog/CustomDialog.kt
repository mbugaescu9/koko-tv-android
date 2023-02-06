package com.kokoconnect.android.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Spannable
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.DialogLayoutBinding

class CustomDialog(context: Context) : Dialog(context) {
    override fun onBackPressed() {
        dismiss()
        super.onBackPressed()
    }

    private var binding: DialogLayoutBinding? = null

    constructor(context: Context, params: Params) : this(context) {
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_layout)
            setCancelable(false)
            setCanceledOnTouchOutside(false)

            if (params.strCancel != null) {
                binding?.cancelButton?.visibility = View.VISIBLE
            } else {
                binding?.cancelButton?.visibility = View.GONE
            }

            if (params.inputVisibility) {
                binding?.inputEditText?.visibility = View.VISIBLE
            } else {
                binding?.inputEditText?.visibility = View.GONE
            }

            params.inputHint?.let {
                binding?.inputEditText?.setHint(it)
            }
            params.title?.let{
                binding?.titleTextView?.setText(it)
            }
            binding?.titleTextView?.visibility = if (params.title != null) View.VISIBLE else View.GONE

            if (params.messageSpannable != null) {
                binding?.messageTextView?.setText(params.messageSpannable)
                binding?.messageTextView?.visibility = View.VISIBLE
            } else if (params.message != null) {
                binding?.messageTextView?.setText(params.message)
                binding?.messageTextView?.visibility = View.VISIBLE
            } else {
                binding?.messageTextView?.visibility = View.GONE
            }

            params.strOk?.let{
                binding?.okButton?.setText(it)
            }
            params.strCancel?.let{
                binding?.cancelButton?.setText(it)
            }

            binding?.okButton?.setOnClickListener {
                try {
                    dismiss()
                    params.onClickListener?.invoke(binding?.inputEditText?.text?.toString() ?: "")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            binding?.cancelButton?.setOnClickListener {
                try {
                    dismiss()
                    params.onClickListener?.invoke(binding?.inputEditText?.text?.toString() ?: "")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    class Builder(){
        var params = Params()
        fun setOkButtonText(text: String): Builder {
            params.strOk = text
            return this
        }
        fun setCancelButtonText(text: String): Builder {
            params.strCancel = text
            return this
        }
        fun setTitle(text: String): Builder {
            params.title = text
            return this
        }
        fun setMessage(text: String): Builder {
            params.message = text
            return this
        }
        fun setMessage(text: Spannable): Builder {
            params.messageSpannable = text
            return this
        }
        fun setOnClickListener(listener: ((String)->Unit)?): Builder {
            params.onClickListener = listener
            return this
        }
        fun setInputEnabled(isEnabled: Boolean): Builder {
            params.inputVisibility = isEnabled
            return this
        }
        fun setInputHint(inputHint: String): Builder {
            params.inputHint = inputHint
            return this
        }

        fun build(context: Context): CustomDialog {
            return CustomDialog(context, params)
        }
    }

    class Params() {
        var title: String? = null
        var message: String? = null
        var messageSpannable: Spannable? = null

        var strOk: String? = null
        var strCancel: String? = null
        var inputVisibility: Boolean = false
        var inputHint: String? = null
        var onClickListener: ((String)->Unit)? = null
    }
}