package com.lz.kotlindemo

import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @auther lvzhao
 * Created on 2020/12/24.
 */
object NetworkEngine {
    private val httpLoggingInterceptor: HttpLoggingInterceptor
    private val okHttpClientBuilder: OkHttpClient.Builder
    val retrofit: Retrofit

    /**
     * 初始化
     */
    init {
        httpLoggingInterceptor =
            HttpLoggingInterceptor(logger = object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    val config = LogUtils.getConfig()
                    config.setBorderSwitch(false)
                    config.isLogHeadSwitch = false
                    LogUtils.dTag("HTTP_TAG", message)
                    config.setBorderSwitch(true)
                    config.isLogHeadSwitch = true
                }
            })
        okHttpClientBuilder = OkHttpClient.Builder()
            .addInterceptor(httpLoggingInterceptor)
            .connectTimeout(15_000, TimeUnit.SECONDS)
            .readTimeout(15_000, TimeUnit.SECONDS)

        retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:8880/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClientBuilder.build())
            .build()
    }


    /**
     * 单个网络请求逻辑
     */
    fun <T> singleRequest(
        request: suspend () -> TestResponseBean<T>,
        success: (data: TestResponseBean<T>) -> Unit = { LogUtils.d("success - $it") },
        failure: (data: TestResponseBean<T>) -> Unit = { LogUtils.d("failure - $it") },
        error: (e: Exception) -> Unit = { LogUtils.d("error - $it") }
    ): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            singleRequestCore(
                request,
                success,
                failure,
                error
            )
        }
    }

    /**
     * 应用层请求核心逻辑
     */
    suspend fun <T> singleRequestCore(
        request: suspend () -> TestResponseBean<T>,
        success: (data: TestResponseBean<T>) -> Unit,
        failure: (data: TestResponseBean<T>) -> Unit,
        error: (e: Exception) -> Unit
    ) {
        request(
            request = request,
            error = error,
            success = {
                if (it.code.equals("0")) {
                    success(it)
                } else {
                    failure(it)
                }
            }
        )
    }

    /**
     * 网络层层请求核心逻辑
     */
    suspend fun <T> request(
        request: suspend () -> T,
        success: (data: T) -> Unit,
        error: (e: Exception) -> Unit
    ) {
        try {
            val data: T = withContext(Dispatchers.IO) { request() }
            if (data != null) {
                success(data)
            } else {
                error(Exception("request data is empty"))
            }
        } catch (e: Exception) {
            LogUtils.e(e)
            error(e)
        }
    }


}