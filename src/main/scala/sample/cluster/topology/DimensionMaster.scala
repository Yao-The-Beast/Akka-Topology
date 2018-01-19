package sample.cluster.topology

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.ArrayBuffer

class DimensionMaster(
				dimension : Int,
				numWorkers : Int
					) extends Actor with akka.actor.ActorLogging{

	//used to store the information of the workers
	//dimension, Map[nodeIdx, MasterNodeRef]
	var workers = Map[Int, ActorRef]()

	//used to store the addresses of NodeMasters for its workers/slaves
	var workersNodeMasters = ArrayBuffer[ActorRef]()

	//the dimension this NodeMaster is responsible for
	val dim : Int = dimension

	var totalOnlineWorkers : Int = 0
	var totalWorkers : Int = numWorkers

	def receive = {
		case msg : SanityCheck => 
			println(s"----DimensionMaster: Sanity Check. Recieve ${msg.s} from ${sender}")
			println(s"----Dimensionmaster: Responbile for Dim = ${dim}.")

		case msg : Echo =>
			sender ! EchoBack()

		case msg : SlavesNodeMasters =>
      		assert(msg.dim == dim)
      		workersNodeMasters = msg.nodeMasters

      	case msg : FindWorkers =>
      		//Dial all the slaves' NodeMaster and ask for the address of its workers
      		for (nodeMaster <- workersNodeMasters){
      			nodeMaster ! RequestWorkerAddress(dim)
      		}

  		case msg : RetrieveWorkerAddress =>
  			//Get the worker address from the specific NodeMaster
  			workers = workers.updated(totalOnlineWorkers, msg.worker)
  			totalOnlineWorkers += 1

  			//if we have enough workers, time to start the DM
  			if (totalOnlineWorkers == totalWorkers){
  				self ! DimensionMasterStart()
  			}

      		
	    case msg : DimensionMasterStart =>
	      	println(s"----DimensionMaster: DimensionMaster ${dim} starts to ping its slaves")
	      	broadcastWorkersAddresses()
	}

	private def broadcastWorkersAddresses() : Unit = {
		for ((idx, worker) <- workers) {
			//println(s"----DimensionMaster: Worker Address: ${worker}")
			worker ! InitWorker(workers)
		}
	}
}