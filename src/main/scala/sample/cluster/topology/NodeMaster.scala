package sample.cluster.topology

import akka.actor.{Actor, ActorRef, ActorSystem, Props, RootActorPath, Terminated}
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.{Cluster, Member}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ArrayBuffer

class NodeMaster() extends Actor with akka.actor.ActorLogging{

  //used to store all the dimension masters created by the nodeMaster
  //dimension, dimensionMasterRef
  var dimensionMastersList = ArrayBuffer[(Int, ActorRef)]()
  //used to store all the workers created by the NodeMaster
  //dimension, workerRef
  var workersList = ArrayBuffer[(Int, ActorRef)]()

  var id = -1

  def receive = {

    case msg : GreetingFromGridMaster => 
      println(s"----NodeMaster: Receive greeting from GridMaster. My id is ${msg.idx}")
      id = msg.idx


    case msg : DimensionMasterAssignment =>
      //Step 4
      //Create childActor "DimensionMaster" based on the DimensionMasterCreation Msg and initialize the DimensionMaster appropriately
      createDimensionMasterRequest(msg.dim, msg.slaves)
      //Step 5
      //Ask the slave nodes' nodeMasters to create correspoinding workers for that specific dimension.
      createWorkersRequest(msg.dim, msg.slaves)


    case msg : DimensionMasterCreation =>
      //debug use
      println(s"----NodeMaster: Create # ${msg.dim} Dimension Master")

      //Create Sub Actor to function as Dimension Master
      val dimensionMaster = context.actorOf(
        Props(
          classOf[DimensionMaster],
          msg.dim
        )
        , "DimensionMaster")
      dimensionMastersList += ((msg.dim, dimensionMaster))

      //debug
      //dimensionMastersList.last._2 ! SanityCheck("Hello from NodeMaster")

      //Send its slaves references to the DimensionMaster
      dimensionMastersList.last._2 ! DimensionMasterInitInfo(msg.dim, msg.slaves)


    case msg : WorkerCreation =>
      //debug use 
      println(s"----NodeMaster: Receive Worker Creation from ${sender} who is responsible for dim ${msg.dim}")

      //Create Sub Actor to function as Worker for a dedicated dimension
      //Important!! Worker has no idea of the grid formation
      //It only knows who his master is. 
      //Yet, the info is actually encoded within the worker's name as children actors cannot have identical names
      val worker = context.actorOf(
        Props(
          classOf[Worker]
        )
        , s"Worker${msg.dim}")
      workersList += ((msg.dim, worker))
      //debug
      workersList.last._2 ! SanityCheck("Hello from NodeMaster")


    case Terminated(a) =>
      log.info(s"----hahahaahah")
  }

  private def createDimensionMasterRequest(dim : Int, slaves : Map[Int, ActorRef]) : Unit = {
    self ! DimensionMasterCreation(dim, slaves)
  }

  private def createWorkersRequest(dim : Int, slaves : Map[Int, ActorRef]) : Unit = {
    for ((idx, thisSlave) <- slaves){
       thisSlave ! WorkerCreation(dim)
    }
  }

}

object NodeMaster {

  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)

    initNodeMaster(port)

  }

  private def initNodeMaster(port: String) = {
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [NodeMaster]")).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ClusterSystem", config)
    
    system.actorOf(Props(classOf[NodeMaster]), name = "NodeMaster")
  }

  def startUp() = {
    main(Array())
  }

  def startUp(port: String) = {
    initNodeMaster(port)
  }

}