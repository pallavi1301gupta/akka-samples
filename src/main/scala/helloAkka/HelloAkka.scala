package helloAkka


import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.io.StdIn
import scala.util.{Success, Failure}
import ExecutionContext.Implicits.global
/**
  * Created by Pallavi Gupta on 21/5/18.
  */
case object GetDateMessage
case object GetIdsFromDatabase

class HelloActor extends Actor {
  def receive = {
    case "hello" => println("Hello Akka")
    case _ => println("Unknown message")
  }
}

class AskActor extends Actor {
  def receive = {
    case GetDateMessage => sender ! System.currentTimeMillis().toString
    case _ => println("unknown message")
  }
}

class FutureResultActor extends Actor {
  def receive = {
    case GetIdsFromDatabase => {
      Future(List(1,2,3)).pipeTo(sender)
    }
    case _ => println("Unknown message")
  }
}

object SampleTest extends App {

  //create the actor system
  val system = ActorSystem("HelloSystem")

  // default Actor constructor
  val helloActor = system.actorOf(Props[HelloActor], name = "helloActor")
  val askActor = system.actorOf(Props[AskActor], name = "askActor")
  val futureResultActor = system.actorOf(Props[FutureResultActor], name = "futureResultActor")

  //send some messages to the HelloActor
  helloActor ! "hello"
  helloActor ! "Hi everyone !!"

  //send some messages to the AskActor, we want a response from it

  // (1) this is one way to "ask" another actor for information
  implicit val timeout = Timeout(5 seconds)
  val future1 = askActor ? GetDateMessage
  val result1 = Await.result(future1, timeout.duration).asInstanceOf[String]
  println(s"result1 = $result1")

  //send some messages to the FutureResultActor, we expect a Future back from it
  val future4: Future[List[Int]] = ask(futureResultActor, GetIdsFromDatabase).mapTo[List[Int]]
  future4 onComplete {
    case Success(result4) =>  println(s"result4 = $result4")
    case Failure(t) => println("An error has occured : " + t.getMessage)
  }

  //shutdown the actor system
  system.terminate()

  StdIn.readLine()
}