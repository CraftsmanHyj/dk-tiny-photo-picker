package com.dakingx.app.photopicker

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dakingx.app.photopicker.config.getFileProviderAuthority
import com.dakingx.app.photopicker.databinding.ActivityMainBinding
import com.dakingx.photopicker.ext.toBitmap
import com.dakingx.photopicker.fragment.PhotoFragment
import com.dakingx.photopicker.fragment.PhotoOpResult
import com.dakingx.photopicker.util.capturePhoto
import com.dakingx.photopicker.util.pickPhoto
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var coroutineScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        coroutineScope = MainScope()

        binding.captureBtn.setOnClickListener { capture() }
        binding.galleryBtn.setOnClickListener { pickFromGallery() }
    }

    override fun onDestroy() {
        coroutineScope.cancel()

        super.onDestroy()
    }

    private fun toast(stringResId: Int) {
        Toast.makeText(this, getString(stringResId), Toast.LENGTH_SHORT).show()
    }

    private fun capture() =
        requestPermission(*PhotoFragment.REQUIRED_PERMISSIONS_FOR_CAPTURE.toTypedArray()) {
            captureActual()
        }

    private fun captureActual() {
        coroutineScope.launch {
            when (val result =
                capturePhoto(supportFragmentManager, getFileProviderAuthority(this@MainActivity))) {
                is PhotoOpResult.Success -> handlePhotoUri(result.uri)
                PhotoOpResult.Failure -> toast(R.string.main_tip_op_fail)
                PhotoOpResult.Cancel -> toast(R.string.main_tip_op_cancel)
            }
        }
    }

    private fun pickFromGallery() =
        requestPermission(*PhotoFragment.REQUIRED_PERMISSIONS_FOR_PICK.toTypedArray()) {
            pickFromGalleryActual()
        }

    private fun pickFromGalleryActual() {
        coroutineScope.launch {
            when (val result =
                pickPhoto(supportFragmentManager, getFileProviderAuthority(this@MainActivity))) {
                is PhotoOpResult.Success -> handlePhotoUri(result.uri)
                PhotoOpResult.Failure -> toast(R.string.main_tip_op_fail)
                PhotoOpResult.Cancel -> toast(R.string.main_tip_op_cancel)
            }
        }
    }

    private fun handlePhotoUri(uri: Uri) {
        val bitmap = uri.toBitmap(this)
        binding.photoIv.setImageBitmap(bitmap)
        //删除裁剪后的图片原图
        contentResolver.delete(uri, null, null)
    }

    private fun requestPermission(vararg permission: String, successAction: () -> Unit) {
//        XXPermissions.with(this).permission(*permission).request(object : OnPermissionCallback {
//            override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
//                if (!allGranted) {
//                    runOnUiThread {
//                        toast(R.string.main_tip_lack_permission)
//                    }
//                    return
//                }
//
//                runOnUiThread {
//                    successAction()
//                }
//            }
//        })

        Dexter.withContext(this)
            .withPermissions(*permission).withListener(
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
                            runOnUiThread {
                                successAction()
                            }
                        } else {
                            runOnUiThread {
                                toast(R.string.main_tip_lack_permission)
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        list: MutableList<PermissionRequest>,
                        token: PermissionToken
                    ) {
                        token.continuePermissionRequest()
                    }
                }).check()
    }
}