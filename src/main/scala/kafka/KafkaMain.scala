package kafka

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

object KafkaMain extends App {
  implicit val system = ActorSystem("my-system")
  implicit val mat = ActorMaterializer()
  val config = system.settings.config.getConfig("akka.kafka.consumer")

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new ByteArrayDeserializer)
//      .withBootstrapServers(bootstrapServers)
//      .withGroupId("group1")
//      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val topic = "test"
  Consumer.plainSource(consumerSettings, Subscriptions.topics(topic))
    .runForeach(println)
}
