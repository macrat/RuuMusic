package jp.blanktar.ruumusic.util


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity


class PermissionManager(val activity: Activity) {
    val hasPermission
        get() = Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    var onResultListener: OnResultListener? = null

    var onGrantedListener = { onResultListener?.onGranted() }

    var onDeniedListener = { onResultListener?.onDenied() }

    fun request() = ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)


    abstract class OnResultListener {
        abstract fun onGranted()
        abstract fun onDenied()
    }


    abstract class Activity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
        val permissionManager = PermissionManager(this)

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            if (requestCode == 0) {
                if (grantResults.size == 0 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionManager.onGrantedListener()
                } else {
                    permissionManager.onDeniedListener()
                }
            }
        }
    }
}

