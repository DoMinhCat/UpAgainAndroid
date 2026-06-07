package com.example.upagain.feat

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.upagain.R
import com.example.upagain.databinding.LoginActivityBinding
import com.example.upagain.databinding.MainActivityBinding
import com.example.upagain.feat.auth.LoginActivity
import com.example.upagain.feat.dashboard.DashboardFragment
import com.example.upagain.feat.post.PostFragment
import com.example.upagain.feat.shop.ShopFragment
import com.example.upagain.util.TokenManager.Companion.KEY_TOKEN
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.showTopSnackbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // SESSION CHECK
        if (!isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // STYLING
        binding.bottomNav.itemIconTintList = ContextCompat.getColorStateList(this, R.color.color_on_surface)
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
            .commit()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val token = sharedPrefs.getString(KEY_TOKEN, null)
        return !token.isNullOrEmpty()
    }
}