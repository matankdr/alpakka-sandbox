import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.cassandra.scaladsl.{CassandraFlow, CassandraSource}
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.driver.core.{Cluster, PreparedStatement, SimpleStatement}

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  val keyspaceName = "my_keyspace"

  implicit val session = Cluster.builder
    .addContactPoint("0.0.0.0")
    .withPort(9042)
    .build
    .connect()

  case class User(firstName: String, lastName: String, age: Integer)

  def read() = {
    val statement = new SimpleStatement(s"SELECT * from $keyspaceName.user")

    CassandraSource(statement)
      .map(row => row)
      .runForeach(println)
  }

  def ingest() = {
    val preparedStatement = session.prepare(s"INSERT INTO $keyspaceName.user(first_name, last_name, age) VALUES (?, ?, ?)")

    val statementBinder =
      (elemToInsert: User, statement: PreparedStatement) => statement.bind(elemToInsert.firstName, elemToInsert.lastName, elemToInsert.age)

    val users = for {
      firstName <- 'a' to 'z'
      lastName <- 'a' to 'z'
      age <- 10 to 20
    } yield User(firstName.toString, lastName.toString, age)

    Source(users)
      .via(CassandraFlow.createUnloggedBatchWithPassThrough(
        2,
        preparedStatement,
        statementBinder,
        (u:User) => u.firstName
    )).runForeach(println)
  }

//  ingest()


  read()

//  val stmt = new SimpleStatement(s"SELECT * FROM $keyspaceName.user").setFetchSize(20)
//
//  val rows = CassandraSource(stmt).runWith(Sink.seq)
//  rows.foreach(println)
//  rows.foreach(r => println(r.size))

}
