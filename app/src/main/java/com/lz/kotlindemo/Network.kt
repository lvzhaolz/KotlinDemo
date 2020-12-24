package com.lz.kotlindemo

import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络请求管理类
 * @auther lvzhao
 * Created on 2020/12/24.
 */
object NetworkManager {
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
        error: (e: Exception) -> Unit = { LogUtils.d("error - $it") },
        isRetry: Boolean = true
    ): INetworkRequestTask<TestResponseBean<T>> {
        return RetrofitNetworkRequestTask(
            isRetry,
            getNetworkRequestWorker(
                request,
                success,
                failure,
                error
            )
        )
    }

    /**
     * 获取网络请求Worker
     * 应用层请求核心逻辑
     */
    fun <T> getNetworkRequestWorker(
        request: suspend () -> TestResponseBean<T>,
        success: (data: TestResponseBean<T>) -> Unit,
        failure: (data: TestResponseBean<T>) -> Unit,
        error: (e: Exception) -> Unit
    ): INetworkRequestWorker<TestResponseBean<T>> {
        return object : INetworkRequestWorker<TestResponseBean<T>> {
            override suspend fun api(): TestResponseBean<T> {
                return request()
            }

            override fun success(
                networkRequestTask: INetworkRequestTask<TestResponseBean<T>>,
                data: TestResponseBean<T>
            ) {
                if (data.code == "0") {
                    success(data)
                } else if (data.code == "2" && networkRequestTask.isRetry()) {
                    networkRequestTask.request()
                } else {
                    failure(data)
                }
            }

            override fun error(
                networkRequestTask: INetworkRequestTask<TestResponseBean<T>>,
                e: Exception
            ) {
                LogUtils.d(networkRequestTask, e)
                error(e)
            }

        }
    }
}

/**
 * 基于retrofit的网络请求Task
 */
class RetrofitNetworkRequestTask<T>(
    /**
     * 是否开启重试
     */
    val retry: Boolean = false,
    /**
     * 网络请求Worker
     */
    val iNetworkRequestWorker: INetworkRequestWorker<T>
) : INetworkRequestTask<T> {
    /**
     * 协程Job
     */
    var coroutineJob: Job? = null

    /**
     * 重试次数
     */
    var retryNum: Int = 0

    companion object {
        /**
         * 默认最大重试次数
         */
        val MAX_RETRY_NUM: Int = 2
    }

    /**
     * 请求调用
     */
    override fun request() {
        coroutineJob = CoroutineScope(Dispatchers.Main).launch {
            requestCore()
        }
    }

    /**
     * 是否重试
     */
    override fun isRetry(): Boolean {
        return retry && retryNum < MAX_RETRY_NUM
    }

    /**
     * 网络层层请求核心逻辑
     */
    suspend fun requestCore() {
        try {
            val data = withContext(Dispatchers.IO) { iNetworkRequestWorker.api() }
            if (data != null) {
                iNetworkRequestWorker.success(this, data)
            } else {
                iNetworkRequestWorker.error(this, Exception("request data is empty"))
            }
        } catch (e: Exception) {
            LogUtils.e(e)
            iNetworkRequestWorker.error(this, e)
        }
    }

}

/**
 * 网络请求Worker 接口
 */
interface INetworkRequestWorker<T> {
    suspend fun api(): T
    fun success(networkRequestTask: INetworkRequestTask<T>, data: T)
    fun error(networkRequestTask: INetworkRequestTask<T>, e: Exception)
}

/**
 * 网络请求Task 接口
 */
interface INetworkRequestTask<T> {
    fun request()
    fun isRetry(): Boolean
}
