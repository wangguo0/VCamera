package top.nightfarmer.vcamera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private var mUri: Uri? = null

    private var menuPay: android.view.MenuItem? = null

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 注册 permission launcher（用于 CAMERA 权限）
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                capturePhoto()
            }
        }

        // 注册相机返回处理
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    iv_demo.visibility = android.view.View.VISIBLE
                    vv_demo.visibility = android.view.View.GONE
                    val returnedUri = result.data?.data ?: mUri
                    if (returnedUri != null) {
                        Glide.with(this).load(returnedUri).into(iv_demo)
                    }
                }
            })

        // 注册录像返回处理
        videoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    vv_demo.visibility = android.view.View.VISIBLE
                    iv_demo.visibility = android.view.View.GONE
                    val returnedUri = result.data?.data ?: mUri
                    if (returnedUri != null) {
                        vv_demo.setVideoURI(returnedUri)
                        vv_demo.setOnPreparedListener {
                            it.isLooping = true
                            vv_demo.start()
                        }
                    }
                }
            })

        to_take_pic.setOnClickListener {
            val openCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (hasPreferredApplication(this@MainActivity, openCameraIntent)) {
                startAppDetails(openCameraIntent)
                return@setOnClickListener
            }

            val cameraPermission = Manifest.permission.CAMERA
            if (ContextCompat.checkSelfPermission(this, cameraPermission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(cameraPermission)
                return@setOnClickListener
            }

            capturePhoto()
        }

        to_take_video.setOnClickListener {
            val openCameraIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            if (hasPreferredApplication(this@MainActivity, openCameraIntent)) {
                startAppDetails(openCameraIntent)
                return@setOnClickListener
            }

            val cameraPermission = Manifest.permission.CAMERA
            if (ContextCompat.checkSelfPermission(this, cameraPermission) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(cameraPermission)
                return@setOnClickListener
            }

            val path = filesDir.toString() + File.separator + "images" + File.separator
            val file = File(path, "vcamera-${System.currentTimeMillis()}.mp4")
            if (!file.parentFile.exists()) file.parentFile.mkdirs()

            mUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "$packageName.files", file)
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            videoLauncher.launch(intent)
        }
    }

    private fun capturePhoto() {
        val openCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (openCameraIntent.resolveActivity(packageManager) != null) {
            val path = filesDir.toString() + File.separator + "images" + File.separator
            val file = File(path, "vcamera-${System.currentTimeMillis()}.jpg")
            if (!file.parentFile.exists()) file.parentFile.mkdirs()

            mUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "$packageName.files", file)
            } else {
                Uri.fromFile(file)
            }

            openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
            openCameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cameraLauncher.launch(openCameraIntent)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menuPay = menu?.findItem(R.id.mn_pay)

        val sharedPreferences = getSharedPreferences("db", Context.MODE_PRIVATE)
        val hideIcon = sharedPreferences.getBoolean("hideIcon", false)
        menuPay?.isVisible = !hideIcon

        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.mn_pay) {
            startActivity(Intent(this, PayActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        checkDefaultApp()

        val sharedPreferences = getSharedPreferences("db", Context.MODE_PRIVATE)
        val hideIcon = sharedPreferences.getBoolean("hideIcon", false)
        menuPay?.isVisible = !hideIcon
    }

    private fun checkDefaultApp() {
        tv_notice.text = ""
        tv_notice.visibility = android.view.View.GONE
        val openCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (hasPreferredApplication(this, openCameraIntent)) {
            to_take_pic.setText(R.string.to_reset_default_app)
            tv_notice.setText(R.string.default_app_notice)
            tv_notice.visibility = android.view.View.VISIBLE
        } else {
            to_take_pic.setText(R.string.image_capture_test)
        }
        val openCameraIntent2 = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (hasPreferredApplication(this, openCameraIntent2)) {
            to_take_video.setText(R.string.to_reset_default_app)
            tv_notice.setText(R.string.default_app_notice)
            tv_notice.visibility = android.view.View.VISIBLE
        } else {
            to_take_video.setText(R.string.video_capture_test)
        }
    }

    private fun startAppDetails(intent: Intent) {
        val pm = packageManager
        val info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val intent2 = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${info.activityInfo.packageName}"))
        startActivity(intent2)
    }

    private fun hasPreferredApplication(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager
        val info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return "android" != info.activityInfo.packageName
    }
}
