package com.sunnada.kotlindemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val httpLoggingInterceptor = HttpLoggingInterceptor(logger = object :
            HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                val config = LogUtils.getConfig()
                config.setBorderSwitch(false)
                config.isLogHeadSwitch = false
                LogUtils.dTag("HTTP_TAG", message)
                config.setBorderSwitch(true)
                config.isLogHeadSwitch = true
            }
        })
        val okHttpClientBuilder = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .connectTimeout(15_000, TimeUnit.SECONDS)
            .readTimeout(15_000, TimeUnit.SECONDS)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:10083/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClientBuilder.build())
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            val result = networkSingleRequest2 { retrofit.create(Api::class.java).test2() }
            LogUtils.d("out", result)
        }


    }

}

fun <T : TestResponseBean> networkSingleRequest(
    success: (data: T) -> Unit = { LogUtils.d("success - $it") },
    failure: (data: T) -> Unit = { LogUtils.d("success - $it") },
    error: (e: Exception) -> Unit = { LogUtils.d("success - $it") },
    block: suspend () -> T
): Job {
    return CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.IO) {
            try {
                val data: T = block()
                if (data.code.equals("0")) {
                    success(data)
                } else {
                    failure(data)
                }
            } catch (e: Exception) {
                LogUtils.e(e)
                error(e)
            }
        }
    }
}

suspend fun <T : TestResponseBean> networkSingleRequest2(
    success: (data: T) -> Unit = { LogUtils.d("success - $it") },
    failure: (data: T) -> Unit = { LogUtils.d("success - $it") },
    error: (e: Exception) -> Unit = { LogUtils.d("success - $it") },
    block: suspend () -> T
) {
    return withContext(Dispatchers.IO) {
        try {
            val data: T = block()
            if (data.code.equals("0")) {
                success(data)
            } else {
                failure(data)
            }
        } catch (e: Exception) {
            LogUtils.e(e)
            error(e)
        }
    }
}

interface Api {

    @GET("/test")
    suspend fun test(): TestResponseBean

    @GET("/test2")
    suspend fun test2(): TestResponseBean

    @GET("/test3")
    suspend fun test3(): TestResponseBean

}

data class TestResponseBean(
    var code: String,
    var data: String,
    var message: String
)
