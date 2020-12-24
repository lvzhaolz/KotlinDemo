package com.lz.kotlindemo

import kotlin.reflect.full.declaredFunctions

/**
 * @auther lvzhao
 * Created on 2020/12/21.
 */
class KTDemo {

}

fun main() {
    A().let {
        println("A let")
    }

    var a = A()

    a.apply {
        println("a apply")
    }

    val funList = B()::class.declaredFunctions
    println("annotations - ${funList.first().annotations}")
    funList.forEach { item ->
        item.annotations.forEach {
            if (it is AA) {
                println("anno value - ${it.msg}")
            }
        }
    }

}

fun A() {
    println("A")
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
//@MustBeDocumented
annotation class AA(val msg: String = "default") {

}


class B {
    @AA("custom")
    fun b() {
        println("b")
    }
}