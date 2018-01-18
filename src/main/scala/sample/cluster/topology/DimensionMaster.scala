package sample.cluster.topology

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.ArrayBuffer

class DimensionMaster(
				dimension : Int
					) extends Actor with akka.actor.ActorLogging{

	//used to store the information of the workers
	//dimension, Map[nodeIdx, MasterNodeRef]
	var workers = Map[Int, ActorRef]()

	//the dimension this NodeMaster is responsible for
	val dim : Int = dimension

	def receive = {
		case msg : SanityCheck => 
			println(s"---DimensionMaster: Sanity Check. Recieve ${msg.s} from ${sender}")
			println(s"---Dimensionmaster: Responbile for Dim = ${dim}.")

		case msg : DimensionMasterInitInfo =>
      		assert(msg.dim == dim)
      		workers = msg.slaves
      		//debug
      		workers.foreach(p => println(s"----DimensionMaster for Dim: ${dim}: workers index = ${p._1} ~~~ ${p._2}"))
      	}
}