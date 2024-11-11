package jixiejidiguan.http

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URL

data class RequestParams(
    val url: String,
    val userAgent: String,
    val header: String,
    val referer: String,
    val postData: String,
    val cookie: String
)

class MainActivity2 : AppCompatActivity() {

    private lateinit var httpClient: OkHttpClient
    private lateinit var notificationManager: NotificationManagerCompat
    private val channelId = "http_request_channel"
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted, proceed with the request
            sendRequest()
        } else {
            // Permission is denied, show a dialog to explain why the permission is needed
            Log.e("MainActivity2", "POST_NOTIFICATIONS permission denied")
            Toast.makeText(this, "POST_NOTIFICATIONS permission denied", Toast.LENGTH_SHORT).show()
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)

        // 初始化 HttpClient
        httpClient = OkHttpClient()

        // 初始化 NotificationManager
        notificationManager = NotificationManagerCompat.from(this)

        // 创建通知渠道
        createNotificationChannel()

        // 设置边缘到边缘显示
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 设置按钮点击事件
        findViewById<Button>(R.id.button2).setOnClickListener {
            if (checkNotificationPermission()) {
                sendRequest()
            }
        }
    }

    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                return true
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return false
            }
        }
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "HTTP Request Channel"
            val descriptionText = "Channel for HTTP request notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendRequest() {
        val requestParams = getRequestParamFromViews()

        if (requestParams.url.isEmpty()) {
            Log.e("MainActivity2", "URL cannot be empty")
            Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
            showNotification("Error", "URL cannot be empty")
            return
        }

        try {
            URL(requestParams.url) // 尝试解析 URL
        } catch (e: Exception) {
            Log.e("MainActivity2", "Invalid URL format: ${requestParams.url}")
            Toast.makeText(this, "Invalid URL format: ${requestParams.url}", Toast.LENGTH_SHORT).show()
            showNotification("Error", "Invalid URL format: ${requestParams.url}")
            return
        }

        if (requestParams.userAgent.isEmpty()) {
            Log.e("MainActivity2", "All request parameters must be provided")
            Toast.makeText(this, "All request parameters must be provided", Toast.LENGTH_SHORT).show()
            showNotification("Error", "All request parameters must be provided")
            return
        }

        val requestBody = FormBody.Builder()
            .add("post", requestParams.postData)
            .build()

        val requestBuilder = Request.Builder()
            .url(requestParams.url)
            .header("User-Agent", requestParams.userAgent)

        if (requestParams.header.isNotEmpty()) {
            requestBuilder.header("Header", requestParams.header)
        }
        if (requestParams.referer.isNotEmpty()) {
            requestBuilder.header("Referer", requestParams.referer)
        }
        if (requestParams.cookie.isNotEmpty()) {
            requestBuilder.header("Cookie", requestParams.cookie)
        }

        val request = if (requestParams.postData.isNotEmpty()) {
            requestBuilder.post(requestBody).build().also {
                Log.d("MainActivity2", "Sending POST request")
                Log.d("MainActivity2", "Request body: ${requestBody.contentLength()} bytes")
            }
        } else {
            requestBuilder.get().build().also {
                Log.d("MainActivity2", "Sending GET request")
                Log.d("MainActivity2", "Request body: 0 bytes")
            }
        }


        httpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity2", "Request failed: ${e.message}", e)
                Toast.makeText(applicationContext, "Request failed: ${e.message}", Toast.LENGTH_SHORT).show()
                showNotification("Error", "Request failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody == null) {
                        Log.e("MainActivity2", "Response body is null")
                        runOnUiThread {
                            Toast.makeText(applicationContext, "Response body is null", Toast.LENGTH_SHORT).show()
                        }
                        showNotification("Error", "Response body is null")
                        return
                    }
                    Log.d("MainActivity2", "Response: $responseBody")
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Success", Toast.LENGTH_SHORT).show()
                        showNotification("Success", "Request successful: ")
                        showAlertDialog("Response", responseBody)
                    }
                } else {
                    Log.e("MainActivity2", "Request failed: ${response.code} - ${response.message}")
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                        showNotification("Error", "Request failed: ${response.code} - ${response.message}")
                    }
                }
                response.body?.close() // 关闭响应体
            }

        })
    }

    private fun getRequestParamFromViews(): RequestParams {
        return RequestParams(
            url = findViewById<EditText>(R.id.editText1).text.toString(),
            userAgent = findViewById<EditText>(R.id.editText2).text.toString(),
            header = findViewById<EditText>(R.id.editText3).text.toString(),
            referer = findViewById<EditText>(R.id.editText4).text.toString(),
            postData = findViewById<EditText>(R.id.editText5).text.toString(),
            cookie = findViewById<EditText>(R.id.editText6).text.toString(),
        )
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(1, builder.build())
    }


    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限请求")
            .setMessage("为了发送通知，我们需要 POST_NOTIFICATIONS 权限。请在设置中开启此权限。")
            .setPositiveButton("去设置") { _, _ ->
                // 打开应用设置页面
                val intent = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, null)
                val settingsIntent = Intent(intent, uri)
                startActivity(settingsIntent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAlertDialog(title: String, message: String) {
        val isJson = try {
            JSONObject(message)
            true
        } catch (e: Exception) {
            false
        }
        val formattedContent = if (isJson) {
            JSONObject(message).toString(4) // 格式化 JSON 输出
        } else {
            message
        }
        buildAndShowAlertDialog(this, title, formattedContent)
    }

    private fun buildAndShowAlertDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("复制") { dialog, _ ->
                copyToClipboard(context, message)
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("关闭") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("label", text)
        clipboardManager.setPrimaryClip(clipData)
    }
}
