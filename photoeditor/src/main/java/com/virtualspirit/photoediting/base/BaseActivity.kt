package com.virtualspirit.photoediting.base

import android.R
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

/**
 * Created by Burhanuddin Rashid on 1/17/2018.
 */
open class BaseActivity : AppCompatActivity() {
    private var mProgressDialog: ProgressDialog? = null
    private var mPermission: String? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isPermissionGranted(it, mPermission)
    }

    fun requestPermission(permission: String): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (!isGranted) {
            mPermission = permission
            permissionLauncher.launch(permission)
        }
        return isGranted
    }

    open fun isPermissionGranted(isGranted: Boolean, permission: String?) {}

    fun makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        // FLAG_FULLSCREEN is deprecated since API 30 and ignored on Android 12+.
        // It also causes systemBars.top to report 0, which breaks WindowInsets-based
        // padding. WindowCompat.setDecorFitsSystemWindows(false) is the modern
        // equivalent: it enables edge-to-edge drawing and correctly dispatches
        // system bar insets so callers can apply the right top padding.
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    protected fun showLoading(message: String) {
        mProgressDialog = ProgressDialog(this)
        mProgressDialog?.run {
            setMessage(message)
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            setCancelable(false)
            show()
        }
    }

    protected fun hideLoading() {
        mProgressDialog?.dismiss()
    }

    protected fun showSnackbar(message: String) {
        val view = findViewById<View>(R.id.content)
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}