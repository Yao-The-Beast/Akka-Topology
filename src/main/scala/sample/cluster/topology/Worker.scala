package sample.cluster.topology

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.ArrayBuffer

class Worker(omaewa: String) extends Actor with akka.actor.ActorLogging{

	var peers = Map[Int, ActorRef]()
	var master : Option[ActorRef] = None
	val name : String = omaewa

	def receive = {
		case msg : SanityCheck => 
			println(s"----Worker: Sanity Check for ${self} . Message Recieve '${msg.s}'") 

		case msg : Echo =>
			sender ! EchoBack()

		case msg : InitWorker =>
			peers = msg.peers
			master = Some(sender)
			//debug
			println(s"----Worker: Name is ${name}")
			peers.foreach(p => println(s"----Worker: Peers are: ${p._1}  ${p._2}"))
			println(s"----Worker: Master is ${master}")
	}
}