package com.example.upagain.feat.error

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.upagain.R
import com.example.upagain.databinding.ErrorActivityBinding

class ErrorActivity : AppCompatActivity() {
    private lateinit var binding: ErrorActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ErrorActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, 500)
        hydrateErrorScreen(errorCode)

        binding.btnErrorAction.setOnClickListener {
            finish()
        }

    }

    // PRIVATE ZONE
    private fun hydrateErrorScreen(code: Int) {
        binding.tvErrorCode.text = code.toString()

        when (code) {
            403 -> {
                binding.tvErrorTitle.text = getString(R.string.error_403_description)
                binding.tvErrorMessage.text = getString(R.string.error_403_msg)
            }
            404 -> {
                binding.tvErrorTitle.text = getString(R.string.error_404_description)
                binding.tvErrorMessage.text = getString(R.string.error_404_msg)
            }
            500 -> {
                binding.tvErrorTitle.text = getString(R.string.error_500_description)
                binding.tvErrorMessage.text = getString(R.string.error_500_msg)
            }
            else -> {
                binding.tvErrorTitle.text = getString(R.string.error_generic_title)
                binding.tvErrorMessage.text = getString(R.string.exception_message)
            }
        }
    }

    companion object {
        private const val EXTRA_ERROR_CODE = "EXTRA_ERROR_CODE"

        /**
         * Safe factory method configuration block to invoke this screen structure cleanly
         */
        @JvmStatic
        fun start(context: Context, statusCode: Int) {
            val intent = Intent(context, ErrorActivity::class.java).apply {
                putExtra(EXTRA_ERROR_CODE, statusCode)
            }
            context.startActivity(intent)
        }
    }
}