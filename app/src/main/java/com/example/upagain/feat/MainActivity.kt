package com.example.upagain.feat

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.upagain.R
import com.example.upagain.databinding.MainActivityBinding
import com.example.upagain.feat.auth.LoginActivity
import com.example.upagain.feat.container.ContainerFragment
import com.example.upagain.feat.dashboard.DashboardFragment
import com.example.upagain.feat.error.NoConnectionActivity
import com.example.upagain.feat.post.fragment.PostFragment
import com.example.upagain.feat.profile.ProfileFragment
import com.example.upagain.feat.shop.ShopFragment
import com.example.upagain.util.auth.SessionManager
import com.example.upagain.util.network.NetworkMonitor
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // CHECK CONNECTION
        networkMonitor = NetworkMonitor(applicationContext)
        observeNetworkState()

        // SESSION CHECK
        SessionManager.init(this)
        if (!SessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // STYLING
        binding.bottomNav.itemIconTintList =
            ContextCompat.getColorStateList(this, R.color.color_on_surface)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.bottomNav.updatePadding(bottom = systemBars.bottom)
            insets
        }

        // 1. Set the default fragment on first load
        if (savedInstanceState == null) {
            val justLoggedIn = intent.getBooleanExtra("EXTRA_JUST_LOGGED_IN", false)
            replaceFragment(DashboardFragment.newInstance(justLoggedIn))
        }

        // 2. Set the listener for clicks
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_shop -> {
                    replaceFragment(ShopFragment())
                    true
                }

                R.id.nav_container -> {
                    replaceFragment(ContainerFragment())
                    true
                }

                R.id.nav_dashboard -> {
                    replaceFragment(DashboardFragment())
                    true
                }

                R.id.nav_community -> {
                    replaceFragment(PostFragment())
                    true
                }

                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }

                else -> false
            }
        }

    }

    // Helper function to handle the transaction of replacing fragment
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)

                // If the user tapped outside the bounding box of the active EditText
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()

                    // Hide the soft keyboard safely
                    WindowCompat.getInsetsController(window, v).hide(
                        WindowInsetsCompat.Type.ime()
                    )
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun observeNetworkState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isConnected.collect { isConnected ->
                    if (!isConnected) {
                        navigateToErrorPage()
                    }
                }
            }
        }
    }

    private fun navigateToErrorPage() {
        val intent = Intent(this, NoConnectionActivity::class.java).apply {
            // Prevent multiple instances of ErrorActivity from piling up
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}