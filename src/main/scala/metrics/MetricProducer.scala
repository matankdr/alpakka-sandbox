package metrics

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import kamon.Kamon

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

object MetricProducer extends App {
  implicit val system = ActorSystem("system")
  implicit val mat = ActorMaterializer()

  Kamon.init()

  val successes = Kamon.counter("success").withoutTags()
  val failures  = Kamon.counter("failures").withoutTags()

  def doSomething() = {
    val randomValue = Random.nextBoolean()

    if (randomValue) successes.increment()
    else failures.increment()
  }

  val r = new Runnable {
    override def run(): Unit = doSomething()
  }
  system.scheduler.schedule(1 second, 500 millis, r)
}
