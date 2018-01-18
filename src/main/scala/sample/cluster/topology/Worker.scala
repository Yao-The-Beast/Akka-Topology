package sample.cluster.topology

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.ArrayBuffer

class Worker() extends Actor with akka.actor.ActorLogging{
	def receive = {
		case msg : SanityCheck => 
			println(s"----Worker: Sanity Check for ${self} . Message Recieve '${msg.s}'") 

		case msg : GreetingFromDimensionMaster =>
			

	}
}