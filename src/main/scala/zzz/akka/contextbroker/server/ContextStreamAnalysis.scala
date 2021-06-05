package zzz.akka.contextbroker.server

import akka.Done
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{CompletionStrategy, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Keep, Sink, Source}
import zzz.akka.contextbroker.server.ContextServerApp.system.executionContext
import zzz.akka.contextbroker.server.ContextServerApp.system

import scala.concurrent.duration.DurationInt
import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorSystem}
import akka.actor.TypedActor.context
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths
import akka.actor.typed.ActorRef
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import zzz.akka.contextbroker.server.ContextSupervisor.StreamMsg

import scala.util.{Failure, Success}
object ContextStreamAnalysis {
//// analysis: numero de mensajes por segundo, media de los valores, max, min,
  def apply():Behavior[StreamMsg] = Behaviors.setup{ ctx =>

    implicit val mat: Materializer = Materializer(ctx)

    trait Protocol
    case class Message(msg: String) extends Protocol
    case class ContextVal(ctxVal: Double) extends Protocol
    case object Complete extends Protocol
    case class Fail(ex: Exception) extends Protocol

    val source: Source[Protocol, ActorRef[Protocol]] = ActorSource.actorRef[Protocol](completionMatcher = {
      case Complete =>
    }, failureMatcher = {
      case Fail(ex) => ex
    }, bufferSize = 8, overflowStrategy = OverflowStrategy.fail)

//    val entity = ctx.spawn(ContextBrokerEntity(),"context-entity")


    // materialize the flow and get the value of the FoldSink

//    val ref = source
//      .collect {
//        case Message(msg) => msg
//      }
//      .toMat(Sink.foreach(println))
//      .run()

//    val bufferSize = 1000
//
//    val od = Source
//      .queue[Int](bufferSize)
//      .via(Flow[Int].map(_ => 1))
//      .runWith(Sink.fold[Int,Int](0)(_+_))
//    val (queue,valMat) = od.run()
//    valMat.foreach(c => println(s"Total tweets processed: $c"))
//    val fastElements = 1 to 10

//    fastElements.foreach { x =>
//      queue.offer(x) match {
//        case QueueOfferResult.Enqueued    => println(s"enqueued $x")
//        case QueueOfferResult.Dropped     => println(s"dropped $x")
//        case QueueOfferResult.Failure(ex) => println(s"Offer failed ${ex.getMessage}")
//        case QueueOfferResult.QueueClosed => println("Source Queue closed")
//      }
//    }



    Behaviors.receiveMessage {
      case StreamMsg (values: List[List[String]]) =>
        val completeWithDone: PartialFunction[Any, CompletionStrategy] = { case Done => CompletionStrategy.immediately }
        val matValuePoweredSource =
          Source.actorRef[Int](
            completionMatcher = completeWithDone,
            failureMatcher = PartialFunction.empty,
            bufferSize = 100,
            overflowStrategy = OverflowStrategy.fail)

        val (actorRef, sourceX) = matValuePoweredSource.preMaterialize()
        // pass source around for materialization
        val pepe = sourceX.runWith(Sink.fold[Int,Int](0)(_+_))
        pepe onComplete {
//          case Success(res) => entity ! pepe
          case Failure(_) => sys.error("something wrong")
        }
        actorRef ! 1
        actorRef ! 1
        actorRef ! 1
        val x = List(values(0)(1).split(":").toList(1).toInt,values(1)(1).split(":").toList(1).toInt,values(2)(1).split(":").toList(1).toInt)
//        ref ! ContextVal(values(0)(1).split(":").toList(1).toDouble)
//        x.foreach { x =>
//          queue offer (x) match {
//            case QueueOfferResult.Enqueued => println(s"enqueued $x")
//            case QueueOfferResult.Dropped => println(s"dropped $x")
//            case QueueOfferResult.Failure(ex) => println(s"Offer failed ${ex.getMessage}")
//            case QueueOfferResult.QueueClosed => println("Source Queue closed")
//          }
//        }
//
//        valMat.foreach(c => println(s"Total tweets processed: $c"))
        Behaviors.same
  }

}

/////////////// example factorial
//  implicit val system: ActorSystem = ActorSystem("reactive-tweets")
//  val source: Source[Int, NotUsed] = Source(1 to 100)
//  val done: Future[Done] = source.runForeach(i => println(i))
//
//  implicit val ec = system.dispatcher
//  done.onComplete(_ => system.terminate())
//def lineSink(filename: String): Sink[String, Future[IOResult]] =
//  Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
//  factorials.map(_.toString).runWith(lineSink("factorial2.txt"))


/////////////////////////////example tweets
  //final case class Author(handle: String)
//
//  final case class Hashtag(name: String)
//
//  final case class Tweet(author: Author, timestamp: Long, body: String) {
//    def hashtags: Set[Hashtag] =
//      body
//        .split(" ")
//        .collect {
//          case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
//        }
//        .toSet
//  }
//  val tweets: Source[Tweet, NotUsed] = Source(
//    Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
//      Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
//      Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
//      Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
//      Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
//      Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
//      Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
//      Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
//      Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
//      Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
//      Nil)
//
//  implicit val system: ActorSystem = ActorSystem("reactive-tweets")
//
//  val akkaTag = Hashtag("#akka")
//  val count: Flow[Tweet, Int, NotUsed] = Flow[Tweet].map(_ => 1)
//
//  val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
//
//  val counterGraph: RunnableGraph[Future[Int]] =
//    tweets.via(count).toMat(sumSink)(Keep.right)
//
//  val sum: Future[Int] = counterGraph.run()
//
//  sum.foreach(c => println(s"Total tweets processed: $c"))
  /////////////////////////
  //      val bufferSize = 10
//
//      val cm: PartialFunction[Any, CompletionStrategy] = {
//        case Done =>
//          CompletionStrategy.immediately
//      }
//
//      val ref = Source
//        .actorRef[Int](
//          completionMatcher = cm,
//          failureMatcher = PartialFunction.empty[Any, Throwable],
//          bufferSize = bufferSize,
//          overflowStrategy = OverflowStrategy.fail) // note: backpressure is not supported
//        .map(x => x * x)
//        .toMat(Sink.foreach((x: Int) => println(s"completed $x")))(Keep.left)
//        .run()
//
//      ref ! 1
//      ref ! 2
//      ref ! 3
//      ref ! Done

}
