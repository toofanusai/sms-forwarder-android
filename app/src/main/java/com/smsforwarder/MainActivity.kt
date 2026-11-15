package com.smsforwarder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smsforwarder.databinding.ActivityMainBinding
import com.smsforwarder.service.SmsForwarderService
import com.smsforwarder.util.NotificationHelper
import com.smsforwarder.util.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, R.string.permissions_required, Toast.LENGTH_LONG).show()
        } else {
            startForwarderService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createChannels(this)
        setupNavigation()
        requestPermissionsIfNeeded()

        // Request battery optimization ignore on first launch
        if (!PermissionHelper.isIgnoringBatteryOptimizations(this)) {
            PermissionHelper.requestBatteryOptimizationIgnore(this)
        }
    }

    private fun setupNavigation() {
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        val bottomNav: BottomNavigationView = binding.bottomNavigation
        bottomNav.setupWithNavController(navController)
    }

    private fun requestPermissionsIfNeeded() {
        val missing = PermissionHelper.missingPermissions(this)
        if (missing.isEmpty()) {
            startForwarderService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startForwarderService() {
        val intent = Intent(this, SmsForwarderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
