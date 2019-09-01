package metrics

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow, CassandraSink}
import akka.stream.scaladsl.{Keep, Source, SourceQueueWithComplete}
import com.datastax.driver.core.{Cluster, PreparedStatement}
import com.typesafe.config.Config
import kamon.metric.PeriodSnapshot
import kamon.module.{MetricReporter, ModuleFactory}

class CassandraMetricsFactory extends ModuleFactory {
  override def create(settings: ModuleFactory.Settings) = {
    implicit val system =  ActorSystem("Sdf")
    implicit val mat: Materializer = ActorMaterializer()
    CassandraReporter()
  }

//  override def create(settings: ModuleFactory.Settings): module.Module = {
}

object CassandraReporter {
  case class PiplMetric(label: String, count: java.lang.Integer)
  val keyspaceName = "my_keyspace"
  val metricsTable = "metrics"

  def apply()(implicit system: ActorSystem, mat: Materializer): CassandraReporter = {
    new CassandraReporter()(system, mat)
  }
}

class CassandraReporter(implicit system: ActorSystem, mat: Materializer) extends MetricReporter {
  import CassandraReporter._

  implicit val session = Cluster.builder
    .addContactPoint("0.0.0.0")
    .withPort(9042)
    .build
    .connect()

  val preparedStatement = session.prepare(s"INSERT INTO $keyspaceName.$metricsTable(label, count, t) VALUES (?, ?)")
//  val preparedStatement = session.prepare(s"INSERT INTO $keyspaceName.$metricsTable(label, count, t) VALUES (?, ?, toTimeStamp(now()))")

  val statementBinder =
    (metric: PiplMetric, statement: PreparedStatement) => statement.bind(metric.label, metric.count)

  val sink = CassandraSink(
    parallelism = 2,
    statement = preparedStatement,
    statementBinder = statementBinder
  )

  def toPiplMetric(snapshot: PeriodSnapshot): Seq[PiplMetric] = {
    for {
      counter <- snapshot.counters
      instrument <- counter.instruments
    } yield {
      PiplMetric(
        counter.name,
        instrument.value.toInt
      )
    }
  }

  val queue: SourceQueueWithComplete[PeriodSnapshot] = Source.queue[PeriodSnapshot](
    bufferSize = 100,
    overflowStrategy = OverflowStrategy.backpressure
  ).mapConcat(snapshot => toPiplMetric(snapshot).toList)
    .toMat(sink)(Keep.left)
    .run()

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit = {
    queue.offer(snapshot)  // no backpressure!
  }

  override def stop(): Unit = { }

  override def reconfigure(newConfig: Config): Unit = { }
}
