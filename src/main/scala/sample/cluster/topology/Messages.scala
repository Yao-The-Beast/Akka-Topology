package sample.cluster.topology

import akka.actor.ActorRef
import scala.collection.mutable.ArrayBuffer

final case class SanityCheck(s : String)
final case class GreetingFromGridMaster(id : Int)
final case class DimensionMasterAssignment(dim : Int, slaves : ArrayBuffer[ActorRef])
final case class WorkerCreation(dim : Int)
final case class DimensionMasterCreation(dim : Int, slaves : ArrayBuffer[ActorRef])


final case class SlavesNodeMasters(dim : Int, nodeMasters : ArrayBuffer[ActorRef]) //Store the DimensionMasterAssignment in NM class for later use

final case class Echo() //check if the actor is online
final case class EchoBack() //repsonse to the echo

final case class DimensionMasterStart() //Let the DM start pinging its slaves

final case class FindWorkers() //Let the DM find workers based on SlavesNodeMasters Msg
final case class RequestWorkerAddress(dim : Int)
final case class RetrieveWorkerAddress(worker : ActorRef)

final case class InitWorker(peers : Map[Int, ActorRef]) //it should contain other meta data