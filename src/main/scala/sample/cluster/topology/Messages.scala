package sample.cluster.topology

import akka.actor.ActorRef


final case class SanityCheck(s : String)
final case class GreetingFromGridMaster(idx : Int)
final case class DimensionMasterAssignment(dim : Int, slaves : Map[Int, ActorRef])
final case class WorkerCreation(dim : Int)
final case class DimensionMasterCreation(dim : Int, slaves : Map[Int, ActorRef])
final case class DimensionMasterInitInfo(dim : Int, slaves : Map[Int, ActorRef]) //it should contain other meta data like msg size, chunk size 
final case class GreetingFromDimensionMaster()