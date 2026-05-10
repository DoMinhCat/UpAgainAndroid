package com.example.upagain

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private var bottomNav: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)

        val mainView = findViewById<android.view.View>(R.id.main)
        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav?.itemIconTintList = null

        // 1. Set the default fragment on first load
        if (savedInstanceState == null) {
            replaceFragment(ShopFragment()) // TODO: change to home dashboard
        }

        // 2. Set the listener for clicks
        bottomNav?.setOnItemSelectedListener { item ->
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
                    replaceFragment(ContainerFragment())
                    true
                }
                R.id.nav_community -> {
                    replaceFragment(PostFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment()) // TODO: change to home dashboard
                    true
                }
                else -> false
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            bottomNav?.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    // Helper function to handle the transaction of replacing fragment
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}