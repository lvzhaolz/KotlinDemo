package com.lz.kotlindemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
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
        .baseUrl("http://127.0.0.1:8880/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClientBuilder.build())
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        test1.setOnClickListener {
            test1()
        }
        test2.setOnClickListener {
            test2()
        }
        test3.setOnClickListener {
            test3()
        }

    }

    fun test1() {
        CoroutineScope(Dispatchers.Main).launch {
            var vv = retrofit.create(Api::class.java).test1() + retrofit.create(Api::class.java)
                .test2()
            LogUtils.d(vv)
//            networkSingleRequest2(
//                request = {
//                    retrofit.create(Api::class.java).test1() + retrofit.create(Api::class.java)
//                        .test2()
//                },
//                success = {
//                    LogUtils.d(it)
//                }
//            )
        }
    }

    fun test2() {
        CoroutineScope(Dispatchers.Main).launch {
            networkSingleRequest2(
                request = { retrofit.create(Api::class.java).test2() },
                success = {
                    LogUtils.d(it)
                }
            )
        }
    }

    fun test3() {
        CoroutineScope(Dispatchers.Main).launch {
            networkSingleRequest2(
                request = { retrofit.create(Api::class.java).test3() },
                success = {
                    LogUtils.d(it)
                }
            )
        }
    }
}

fun <T> networkSingleRequest(
    request: suspend () -> TestResponseBean<T>,
    success: (data: TestResponseBean<T>) -> Unit = { LogUtils.d("success - $it") },
    failure: (data: TestResponseBean<T>) -> Unit = { LogUtils.d("success - $it") },
    error: (e: Exception) -> Unit = { LogUtils.d("success - $it") }
): Job {
    return CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.IO) {
            try {
                val data: TestResponseBean<T> = request()
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

suspend fun <T> networkSingleRequest2(
    request: suspend () -> TestResponseBean<T>,
    success: (data: TestResponseBean<T>) -> Unit = { LogUtils.d("success - $it") },
    failure: (data: TestResponseBean<T>) -> Unit = { LogUtils.d("failure - $it") },
    error: (e: Exception) -> Unit = { LogUtils.d("error - $it") }
) {
    return withContext(Dispatchers.IO) {
        try {
            val data: TestResponseBean<T> = request()
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

    @GET("/app/test")
    suspend fun test1(): TestResponseBean<String>

    @GET("/app/test2")
    suspend fun test2(): TestResponseBean<String>

    @GET("/app/test3")
    suspend fun test3(): TestResponseBean<String>

}

operator fun <T> TestResponseBean<T>.plus(b: TestResponseBean<T>): Int {
    return 2
}

data class TestResponseBean<T>(
    var code: String,
    var data: T,
    var message: String
)
