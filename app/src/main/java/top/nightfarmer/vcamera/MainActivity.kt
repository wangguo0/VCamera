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
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import java.io.File

class MainActivity : AppCompatActivity() {

    private var mUri: Uri? = null
    private var menuPay: android.view.MenuItem? = null

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    // 视图引用（替代 kotlin-android-extensions）
    private lateinit var ivDemo: ImageView
    private lateinit var vvDemo: VideoView
    private lateinit var toTakePic: Button
    private lateinit var toTakeVideo: Button
    private lateinit var tvNotice: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化视图引用
        ivDemo = findViewById(R.id.iv_demo)
        vvDemo = findViewById(R.id.vv_demo)
        toTakePic = findViewById(R.id.to_take_pic)
        toTakeVideo = findViewById(R.id.to_take_video)
        tvNotice = findViewById(R.id.tv_notice)

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
                    ivDemo.visibility = android.view.View.VISIBLE
                    vvDemo.visibility = android.view.View.GONE
                    val returnedUri = result.data?.data ?: mUri
                    if (returnedUri != null) {
                        Glide.with(this).load(returnedUri).into(ivDemo)
                    }
                }
            })

        // 注册录像返回处理
        videoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    vvDemo.visibility = android.view.View.VISIBLE
                    ivDemo.visibility = android.view.View.GONE
                    val returnedUri = result.data?.data ?: mUri
                    if (returnedUri != null) {
                        vvDemo.setVideoURI(returnedUri)
                        vvDemo.setOnPreparedListener {
                            it.isLooping = true
                            vvDemo.start()
                        }
                    }
                }
            })

        toTakePic.setOnClickListener {
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

        toTakeVideo.setOnClickListener {
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
        // 添加空安全检查
        val resolveInfo = openCameraIntent.resolveActivity(packageManager)
        if (resolveInfo != null) {
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
        tvNotice.text = ""
        tvNotice.visibility = android.view.View.GONE
        val openCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (hasPreferredApplication(this, openCameraIntent)) {
            toTakePic.setText(R.string.to_reset_default_app)
            tvNotice.setText(R.string.default_app_notice)
            tvNotice.visibility = android.view.View.VISIBLE
        } else {
            toTakePic.setText(R.string.image_capture_test)
        }
        val openCameraIntent2 = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (hasPreferredApplication(this, openCameraIntent2)) {
            toTakeVideo.setText(R.string.to_reset_default_app)
            tvNotice.setText(R.string.default_app_notice)
            tvNotice.visibility = android.view.View.VISIBLE
        } else {
            toTakeVideo.setText(R.string.video_capture_test)
        }
    }

    private fun startAppDetails(intent: Intent) {
        val pm = packageManager
        val info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        // 添加空安全检查
        if (info != null) {
            val intent2 = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, 
                Uri.parse("package:${info.activityInfo.packageName}"))
            startActivity(intent2)
        }
    }

    private fun hasPreferredApplication(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager
        val info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        // 添加空安全检查
        return info != null && "android" != info.activityInfo.packageName
    }
}
