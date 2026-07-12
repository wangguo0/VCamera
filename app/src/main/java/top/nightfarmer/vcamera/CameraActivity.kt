package top.nightfarmer.vcamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_camera.*
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.io.IOException

class CameraActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var pickLauncher: ActivityResultLauncher<Intent>

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, getString(R.string.permission_fail_notice), Toast.LENGTH_SHORT).show()
        // 关闭 Activity 或根据业务提示用户
        finish()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        // 权限已授予，继续选择媒体
        chooseRequest()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // 注册 launcher，用于接收 ACTION_PICK 返回
        pickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    // 选中的媒体 Uri
                    val selectedFile: Uri? = result.data!!.data
                    // dstUri 从启动当前 CameraActivity 的 Intent 中传入（EXTRA_OUTPUT）
                    val dstUri: Uri? = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
                    if (selectedFile != null && dstUri != null) {
                        copy(this, selectedFile, dstUri)
                        // 返回给调用方 dstUri（content:// via FileProvider），并授予读权限
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

        to_choose_pic.setOnClickListener {
            choosePic()
        }

        // 直接触发一次选择（保持原行为）
        choosePic()
    }

    private fun choosePic() {
        // 按 Android 版本区分权限：Android 13+ 用 READ_MEDIA_IMAGES，否则用 READ_EXTERNAL_STORAGE
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        // 使用 EasyPermissions 的 PermissionRequest.Builder 构建并请求权限
        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(this, 111, permission)
                .setRationale(getString(R.string.permission_request))
                .setPositiveButtonText(getString(R.string.ok))
                .setNegativeButtonText(getString(R.string.cancel))
                .build()
        )
    }

    private fun chooseRequest() {
        // 根据 ACTION 判断选择图片或视频
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
