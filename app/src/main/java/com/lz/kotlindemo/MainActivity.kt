package com.lz.kotlindemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.http.GET

class MainActivity : AppCompatActivity() {

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
        NetworkEngine.singleRequest(
            request = {
                LogUtils.d(Thread.currentThread().name)
                NetworkEngine.retrofit.create(Api::class.java).test1()
            },
            success = {
                LogUtils.d(it, Thread.currentThread().name)
            }
        )
    }

    fun test2() {
        NetworkEngine.singleRequest(
            request = {
                LogUtils.d(Thread.currentThread().name)
                NetworkEngine.retrofit.create(Api::class.java).test2()
            },
            success = {
                LogUtils.d(it, Thread.currentThread().name)
            }
        )
    }

    fun test3() {
        NetworkEngine.singleRequest(
            request = {
                LogUtils.d(Thread.currentThread().name)
                NetworkEngine.retrofit.create(Api::class.java).test3()
            },
            success = {
                LogUtils.d(it, Thread.currentThread().name)
            }
        )
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
