package zzz.akka.investigation
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object ContextConsumer {
  def apply(max: Int): Behavior[ContextProducer.Greeted] = {
    bot(0, max)
  }

  private def bot(greetingCounter: Int, max: Int): Behavior[ContextProducer.Greeted] =
    Behaviors.receive { (context, message) =>
      val n = greetingCounter + 1
      context.log.info("Greeting {} for {}", n, message.whom)
      if (n == max) {
        Behaviors.stopped
      } else {
        message.from ! ContextProducer.Greet(message.whom, context.self)
        bot(n, max)
      }
    }
}
