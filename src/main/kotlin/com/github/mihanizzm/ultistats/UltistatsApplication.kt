package com.github.mihanizzm.ultistats

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class UltistatsApplication

fun main(args: Array<String>) {
	runApplication<UltistatsApplication>(*args)
}
