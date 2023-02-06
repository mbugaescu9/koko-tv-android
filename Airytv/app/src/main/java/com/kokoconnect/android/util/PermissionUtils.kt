package com.kokoconnect.android.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat


object PermissionUtils {
    private const val DEFAULT_REQUEST_CODE = 21586
    val READ_WRITE_PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    val READ_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    const val READ_WRITE_PERMISSIONS_REQ_CODE = 11001
    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )
    const val CAMERA_PERMISSIONS_REQ_CODE = 11002

    fun isPermissionsGranted(permissions: Map<String, Boolean>): Boolean {
        var granted = true
        for (permission in permissions) {
            granted = granted && permission.value
            if (!granted) break
        }
        return granted
    }

    fun isPermissionGranted(activity: Activity, permission: String): Boolean {
        val result = if (Build.VERSION.SDK_INT >= 23) {
            activity.checkSelfPermission(permission)
        } else {
            ActivityCompat.checkSelfPermission(activity, permission)
        }
        return result == PackageManager.PERMISSION_GRANTED
    }

    fun isPermissionsGranted(activity: Activity, permissions: Array<String>): Boolean {
        var permissionsGranted = true
        for (permission in permissions) {
            permissionsGranted = permissionsGranted && isPermissionGranted(activity, permission)
            if (!permissionsGranted) {
                break
            }
        }
        PackageManager.PERMISSION_DENIED
        return permissionsGranted
    }

    fun filterNonGrantedPermissions(activity: Activity, permissions: Array<String>): List<String> {
        val nonGrantedPermissions = mutableListOf<String>()
        for (permission in permissions) {
            val result = if (Build.VERSION.SDK_INT >= 23) {
                activity.checkSelfPermission(permission)
            } else {
                ActivityCompat.checkSelfPermission(activity, permission)
            }
            if (result != PackageManager.PERMISSION_GRANTED) {
                nonGrantedPermissions.add(permission)
            }
        }
        return nonGrantedPermissions
    }

    fun shouldShowRequestPermissionRationale(
        activity: Activity,
        permissions: Array<String>
    ): Boolean {
        var shouldShow = false
        for (permission in permissions) {
            val result = if (Build.VERSION.SDK_INT >= 23) {
                activity.shouldShowRequestPermissionRationale(permission)
            } else {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
            shouldShow = shouldShow || result
            if (result) {
                break
            }
        }
        return shouldShow
    }

    fun requestPermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int = DEFAULT_REQUEST_CODE
    ) {
        if (Build.VERSION.SDK_INT >= 23) {
            activity.requestPermissions(permissions, requestCode)
        } else {
            return
        }
    }

    fun requestPermissions(
//        launcher: ActivityResultLauncher<Array<out String>>,
      launcher:  ActivityResultLauncher<Array<String>>,
        permissions: Array<String>
    ) {
        if (Build.VERSION.SDK_INT >= 23) {
            launcher.launch(permissions)
        } else {
            return
        }
    }

    fun getCameraPermissions(): Array<String> {
        return listOf(
            PermissionUtils.CAMERA_PERMISSIONS.toList(),
            if (Build.VERSION.SDK_INT >= 29) {
                PermissionUtils.READ_PERMISSIONS.toList()
            } else {
                PermissionUtils.READ_WRITE_PERMISSIONS.toList()
            }
        ).flatten().toTypedArray()
    }

    fun getGalleryPermissions(): Array<String> {
        return listOf(
            if (Build.VERSION.SDK_INT >= 29) {
                PermissionUtils.READ_PERMISSIONS.toList()
            } else {
                PermissionUtils.READ_WRITE_PERMISSIONS.toList()
            }
        ).flatten().toTypedArray()
    }

}