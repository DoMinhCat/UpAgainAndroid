package com.example.upagain.feat

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
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
import com.example.upagain.feat.post.fragment.PostDetailFragment
import com.example.upagain.feat.post.fragment.PostFragment
import com.example.upagain.feat.profile.ProfileFragment
import com.example.upagain.feat.shop.fragment.ShopFragment
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

        // DEEPLINK - Returns true if a payment deep link successfully mounted a fragment
        val isDeepLinkHandled = handleIncomingDeepLink(intent)

        // STYLING
        binding.bottomNav.itemIconTintList =
            ContextCompat.getColorStateList(this, R.color.color_on_surface)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.bottomNav.updatePadding(bottom = systemBars.bottom)
            insets
        }

        // 1. Set the default fragment on first load (ONLY if a deep link didn't handle navigation already)
        if (savedInstanceState == null && !isDeepLinkHandled && supportFragmentManager.findFragmentById(
                R.id.fragment_container
            ) == null
        ) {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // CRUCIAL: Overwrite the Activity intent reference context so old initial intent data isn't evaluated
        setIntent(intent)
        // Catch deep link if the activity was already alive in the background stack
        handleIncomingDeepLink(intent)
    }

    // PRIVATE ZONE
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

                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Parse deep links and returns a Boolean indicating if an ad-booking redirection occurred.
     */
    private fun handleIncomingDeepLink(intent: Intent?): Boolean {
        val uri: Uri? = intent?.data
        if (uri != null && uri.scheme == "upagain" && uri.host == "payment") {
            val paymentStatus = uri.getQueryParameter("payment")

            if (paymentStatus == "success") {
                val fullUriString = uri.toString()

                // Read the destination fragment identifier from the URL parameter
                val targetFragName = uri.getQueryParameter("frag")

                when (targetFragName) {
                    "PostDetailFragment" -> {
                        // Locate the active instance of the detail fragment if it is already visible
                        val activeFragment =
                            supportFragmentManager.findFragmentById(R.id.fragment_container) as? PostDetailFragment

                        if (activeFragment != null && activeFragment.isAdded) {
                            // Scenario A: Fragment is present, push data directly
                            activeFragment.onPaymentSuccessReturned(fullUriString)
                        } else {
                            // Scenario B: Fragment doesn't exist yet (App was closed/recreated/cold start).
                            val postId = uri.getQueryParameter("idPost")?.toIntOrNull() ?: -1
                            val freshFragment =
                                PostDetailFragment.newInstance(postId, fullUriString)

                            // Mount the fragment directly onto the main frame view container
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, freshFragment)
                                .commit()
                        }
                        return true // Handled PostDetailFragment routing successfully
                    }

                    "SomeOtherFragment" -> {
                        // TODO: Handle redirection to other fragments in the future here
                        // val otherId = uri.getQueryParameter("someId")
                        // replaceFragment(SomeOtherFragment.newInstance(otherId))
                        // return true
                        return false
                    }

                    else -> {
                        val defaultFrag = ShopFragment.newInstance()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, defaultFrag)
                            .commit()
                        return false
                    }
                }
            }
        }
        return false
    }
}