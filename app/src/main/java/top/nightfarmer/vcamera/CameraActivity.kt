package top.nightfarmer.vcamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.io.IOException

class CameraActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var pickLauncher: ActivityResultLauncher<Intent>
    private lateinit var toChoosePic: Button

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, getString(R.string.permission_fail_notice), Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        chooseRequest()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // 初始化视图引用
        toChoosePic = findViewById(R.id.to_choose_pic)

        // 注册 launcher，用于接收 ACTION_PICK 返回
        pickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val selectedFile: Uri? = result.data!!.data
                    val dstUri: Uri? = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
                    if (selectedFile != null && dstUri != null) {
                        copy(this, selectedFile, dstUri)
                        val out = Intent()
                        out.data = dstUri
                        out.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setResult(RESULT_OK, out)
                    } else {
                        setResult(RESULT_CANCELED)
                    }
                    finish()
                } else {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            })

        toChoosePic.setOnClickListener {
            choosePic()
        }

        choosePic()
    }

    private fun choosePic() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(this, 111, permission)
                .setRationale(getString(R.string.permission_request))
                .setPositiveButtonText(getString(R.string.ok))
                .setNegativeButtonText(getString(R.string.cancel))
                .build()
        )
    }

    private fun chooseRequest() {
        if (intent.action == MediaStore.ACTION_VIDEO_CAPTURE) {
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            pickLauncher.launch(pickIntent)
        } else {
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickLauncher.launch(pickIntent)
        }
    }

    private fun copy(context: Context, srcUri: Uri, dstUri: Uri) {
        try {
            val outputStream = context.contentResolver.openOutputStream(dstUri) ?: return
            val inputStream = context.contentResolver.openInputStream(srcUri) ?: return
            IoUtils.copy(inputStream, outputStream)
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
