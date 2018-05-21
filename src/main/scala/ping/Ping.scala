package ping

import akka.actor._

// Messages between actors should be immutable, and case classes are great for this purpose.
case object PingMessage
case object PongMessage
case object StartMessage
case object StopMessage

/**
  * Created by Pallavi Gupta on 13/2/18.
  */
class Ping(pong: ActorRef) extends Actor {
  var count = 0
  def incrementAndPrint: Unit = {
    println("incrementAndPrint")
    count += 1
    println("ping")
  }

  // When you implement an Akka actor, you define the receive method, and implement your desired behavior in that method.
  override def receive = {
    case StartMessage =>
      println("============start message called=================")
      incrementAndPrint
      // actor ! message
      pong ! PingMessage
    case PongMessage =>
      println("============pong message called=================")
      incrementAndPrint
      if (count > 5) {
        println("ping sender is "+sender)
        sender ! StopMessage
        println("ping stopped")
        context.stop(self)
      } else {
        println("ping sender is " + sender)
        sender ! PingMessage
      }
  }
}

class Pong extends Actor {
  def receive = {
    case PingMessage =>
      println("============ping message called=================")
      println("pong sender is " + sender)
      println("pong")
      sender ! PongMessage
    case StopMessage =>
      println("============stop message called=================")
      println("pong stopped")
      context.stop(self)
  }
}

// I get everything started by sending a StartMessage to my “ping” actor. After that, the two actors bounce messages back and forth as fast as they can until they stop.
object PingPongTest extends App {
  val system = ActorSystem("PingPongSystem")

  // Props is a ActorRef configuration object, that is immutable, so it is thread safe and fully sharable. Used when creating new actors through.
  val pong = system.actorOf(Props[Pong], name = "pong")
  val ping = system.actorOf(Props(new Ping(pong)), name = "ping")
  // start them going
  ping ! StartMessage
}
