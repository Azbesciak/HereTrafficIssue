package com.example.demo

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import kotlin.system.exitProcess

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

data class RequestData(
		val x: Int,
		val y: Int,
		val z: Int
)

@Service
class Runner(@Value("\${apiKey}") private val apiKey: String) : ApplicationRunner {
    private val client = WebClient.create()
    private fun request(request: RequestData) =
            client.get().uri("https://traffic.ls.hereapi.com/traffic/6.3/incidents/json/${request.z}/${request.x}/${request.y}?apiKey=$apiKey")
                    .retrieve()
                    .bodyToMono(Map::class.java)


    override fun run(args: ApplicationArguments?) {
        val requests = listOf(RequestData(139, 83, 8), RequestData(139, 82, 8))
        Flux.fromIterable(requests)
                .parallel()
                .runOn(Schedulers.elastic())
                .flatMap { req ->
                    println("THREAD ${Thread.currentThread().name} ${System.currentTimeMillis()}")
                    request(req).doOnNext { res ->
                        println("REQUEST:\n $req,\n RESPONSE:\n $res")
                    }
                }
                .doOnComplete {
                    exitProcess(0)
                }
                .subscribe()
    }

}