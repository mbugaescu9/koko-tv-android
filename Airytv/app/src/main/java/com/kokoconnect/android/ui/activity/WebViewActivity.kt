package com.kokoconnect.android.ui.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.kokoconnect.android.R
import com.kokoconnect.android.databinding.ActivityWebviewBinding
import com.kokoconnect.android.util.AppParams
import org.jetbrains.anko.contentView

class WebViewActivity: AppCompatActivity() {

    enum class Type {
        TERMS, POLICY
    }

    companion object{
        const val KEY_RESOURCE_ID = "resid"
        const val KEY_ASSET_PATH = "asset_path"
        const val KEY_ARGS = "args"
        const val ARGS_SEPARATOR = "$"
    }

    private var binding: ActivityWebviewBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }
        setContentView(R.layout.activity_webview)
        contentView?.let{
            binding = ActivityWebviewBinding.bind(it)
        }

        val resId = intent.getIntExtra(KEY_RESOURCE_ID, -1)
        when (resId) {
            Type.TERMS.ordinal, Type.POLICY.ordinal -> {
                val html = when (resId) {
                    Type.TERMS.ordinal -> readFileText("terms_of_use_text.txt")
                    Type.POLICY.ordinal -> readFileText("privacy_policy_text.txt")
                    else -> ""
                } + "\nBuild number: ${AppParams.versionName} (${AppParams.versionCode}) (api ${AppParams.apiVersion})"
                binding?.webView?.loadDataWithBaseURL("http://freetvmovies.tv/", html, "text/html", "UTF-8", null)
            }
            else -> {
                val args = intent.getStringExtra(KEY_ARGS)?.split(ARGS_SEPARATOR)?.toTypedArray()
                val text = if (args == null)resources.getString(resId) else resources.getString(resId, *args)
                binding?.webView?.loadDataWithBaseURL("http://freetvmovies.tv", text, "text/html", "UTF-8", null)
            }
        }
    }

    private fun readFileText(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
}