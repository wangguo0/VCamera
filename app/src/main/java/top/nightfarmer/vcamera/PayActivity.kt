package top.nightfarmer.vcamera

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import top.nightfarmer.vcamera.donate.AlipayDonate

class PayActivity : AppCompatActivity() {

    private lateinit var btAlipay: Button
    private lateinit var btHidePay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay)

        // 初始化视图引用
        btAlipay = findViewById(R.id.bt_alipay)
        btHidePay = findViewById(R.id.bt_hide_pay)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "捐赠"

        btAlipay.setOnClickListener {
            donateAlipay("tsx09794xowzgzxocqfa3ec")
        }

        btHidePay.setOnClickListener {
            val sharedPreferences = getSharedPreferences("db", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("hideIcon", true)
            editor.commit()
            Toast.makeText(this, "感谢使用", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    /**
     * 支付宝支付
     * @param payCode 收款码后面的字符串
     */
    private fun donateAlipay(payCode: String) {
        val hasInstalledAlipayClient = AlipayDonate.hasInstalledAlipayClient(this)
        if (hasInstalledAlipayClient) {
            AlipayDonate.startAlipayClient(this, payCode)
        }
    }
}
