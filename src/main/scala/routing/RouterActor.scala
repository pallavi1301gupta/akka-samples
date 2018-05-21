package routing

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

import akka.actor._
import akka.pattern.ask
import akka.routing._
import akka.util.Timeout

/**
  * Created by pallavi on 21/5/18.
  */

case class FibonacciNumber(val num: Int)

case object WorkMessage

case object Report

class RouterActor(val routingLogic: RoutingLogic) extends Actor {

  val counter: AtomicInteger = new AtomicInteger()

  val routees = Vector.fill(5) {
    val workerCount = counter.getAndIncrement()
    val r = context.actorOf(Props(
      new WorkerActor(workerCount)), name = s"workerActor-$workerCount")
    context watch r
    ActorRefRoutee(r)
  }

  //create a Router based on the incoming class field
  //RoutingLogic which will really determine what type of router
  //we end up with
  var router = Router(routingLogic, routees)

  def receive = {
    case WorkMessage =>
      router.route(WorkMessage, sender())
    case Report => routees.foreach(ref => ref.send(Report, sender()))
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val workerCount = counter.getAndIncrement()
      val r = context.actorOf(Props(
        new WorkerActor(workerCount)), name = s"workerActor-$workerCount")
      context watch r
      router = router.addRoutee(r)
  }
}

class WorkerActor(val id: Int) extends Actor {

  val actName = self.path.name
  var msgCount = 0

  def receive = {
    case WorkMessage => {
      msgCount += 1
      println(s"worker : {$id}, name : ($actName) ->  ($msgCount)")
    }
    case Report => {
      println(s"worker : {$id}, name : ($actName) ->  saw total messages : ($msgCount)")
    }
    case _ => println("Unknown message")
  }
}

class PoolRouterContainerActor(val props: Props, val name: String) extends Actor {

  val router: ActorRef = context.actorOf(props, name)

  def receive = {
    case WorkMessage =>
      implicit val timeout = Timeout(5 seconds)
      val futureResult = router ? FibonacciNumber(10)
      val (actName, result) = Await.result(futureResult, timeout.duration)

      println(s"FibonacciActor : ($actName) came back with result -> $result")
  }
}

class FibonacciActor extends Actor {

  val actorName = self.path.name

  def receive = {
    case FibonacciNumber(number) => {
      println(s"FibonacciActor : ($actorName) -> has been asked to calculate FibonacciNumber")
      val result = fibonacci(number)
      sender ! (actorName, result)
    }
  }

  private def fibonacci(n: Int): Int = {
    def fib(n: Int, b: Int, a: Int): Int = n match {
      case 0 => a
      case _ => fib(n - 1, a + b, b)
    }

    fib(n, 1, 0)
  }
}

object Sample extends App {
//  RunRoutingSample(RoundRobinRoutingLogic())
//  RunRoutingSample(RandomRoutingLogic())
//  RunRoutingSample(SmallestMailboxRoutingLogic())
  RunRoutingSample(BroadcastRoutingLogic())

  //RunScatterGatherFirstCompletedPoolSample()
  RunTailChoppingPoolSample()

  def RunRoutingSample(routingLogic : RoutingLogic) : Unit = {
    val system = ActorSystem("RoutingSystem")
    val actorRef = system.actorOf(Props(
      new RouterActor(routingLogic)), name = "theRouter")

    for (i <- 0 until 10) {
      actorRef ! WorkMessage
      Thread.sleep(1000)
    }
    actorRef ! Report

    StdIn.readLine()
    system.terminate()
  }

  /** RunScatterGatherFirstCompletedPoolSample
    * A router pool that broadcasts the message to all routees, and replies with the first response.
    */
  def RunScatterGatherFirstCompletedPoolSample() : Unit = {

    val supervisionStrategy = OneForOneStrategy() {
      case e => SupervisorStrategy.restart
    }

    val props = ScatterGatherFirstCompletedPool(
      5, supervisorStrategy = supervisionStrategy,within = 10.seconds).
      props(Props[FibonacciActor])

    RunPoolSample(props)
  }

  /** RunTailChoppingPoolSample
    * A router pool with retry logic, intended for cases where a return message is expected in
    * response to a message sent to the routee. As each message is sent to the routing pool, the
    * routees are randomly ordered. The message is sent to the first routee. If no response is received
    * before the `interval` has passed, the same message is sent to the next routee. This process repeats
    * until either a response is received from some routee, the routees in the pool are exhausted, or
    * the `within` duration has passed since the first send. If no routee sends
    * a response in time, a [[akka.actor.Status.Failure]] wrapping a [[akka.pattern.AskTimeoutException]]
    * is sent to the sender.
    */
  def RunTailChoppingPoolSample() : Unit = {

    val supervisionStrategy = OneForOneStrategy() {
      case e => SupervisorStrategy.restart
    }

    val props = TailChoppingPool(5, within = 10.seconds,
      supervisorStrategy = supervisionStrategy,interval = 20.millis)
      .props(Props[FibonacciActor])

    RunPoolSample(props)
  }

  def RunPoolSample(props : Props) : Unit = {
    val system = ActorSystem("RoutingSystem")
    val actorRef = system.actorOf(Props(
      new PoolRouterContainerActor(props,"theRouter")), name = "thePoolContainer")
    actorRef ! WorkMessage
    StdIn.readLine()
    system.terminate()
  }
}