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
  var dimensionMastersList = Map[Int, ActorRef]()
  //used to store all the workers created by the NodeMaster
  //dimension, workerRef
  var workersList = Map[Int, ActorRef]()

  var gridMaster : Option[ActorRef] = None

  var id = -1

  var totalChildren = 3 //need to be fixed!!!!
  var numWorkersPerNode = 2 //need to be fixed!!!!
  var currentOnlineChildren = 0

  def receive = {

    case msg : GreetingFromGridMaster => 
      println(s"----NodeMaster: Receive greeting from GridMaster. My id is ${msg.id}")
      id = msg.id
      gridMaster = Some(sender)


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
          msg.dim,
          numWorkersPerNode
        )
        , s"DimensionMaster_${msg.dim}")
      context.watch(dimensionMaster)

      dimensionMastersList = dimensionMastersList.updated(msg.dim, dimensionMaster)
      
      //relay the msg to the DimensionMaster for it to lookup
      dimensionMastersList(msg.dim) ! SlavesNodeMasters(msg.dim, msg.slaves)
      //wait for DimensionMaster to echoBack
      dimensionMastersList(msg.dim) ! Echo()


    case msg : WorkerCreation =>
      //debug use 
      println(s"----NodeMaster: Receive Worker Creation from ${sender} who is responsible for dim ${msg.dim}")

      //Create Sub Actor to function as Worker for a dedicated dimension
      //Important!! Worker has no idea of the grid formation
      //It only knows who his master is. 
      //Yet, the info is actually encoded within the worker's name as children actors cannot have identical names
      val worker = context.actorOf(
        Props(
          classOf[Worker],
          s"Worker_${msg.dim}"
        )
        , s"Worker_${msg.dim}")
      context.watch(worker)

      workersList = workersList.updated(msg.dim, worker)
      //workersList(msg.dim) ! SanityCheck("Hello from NodeMaster")
      
      //Wait for the worker to echoBack
      workersList(msg.dim) ! Echo()
      


    case msg : RequestWorkerAddress =>
      sender ! RetrieveWorkerAddress(workersList(msg.dim))


    case msg : EchoBack =>
      currentOnlineChildren += 1
      if (currentOnlineChildren == totalChildren){
        println(s"----NodeMaster: All ${totalChildren} children are online")
        //let GridMaster know I am ready (all my children are online)
        gridMaster.orNull ! NodeMasterReady(id)
      }

    case msg : StartNodeMaster =>
      //Let all the dimension masters start finding their workers
      println(s"----NodeMaster: DimensionMasters Begin to find workers")
      for ((idx, dimensionMaster) <- dimensionMastersList){
        dimensionMaster ! FindWorkers()
      }


    case Terminated(child) =>
      log.info(s"----NodeMaster: Child Actor ${child} is terminated")
  }

  private def createDimensionMasterRequest(dim : Int, slaves : ArrayBuffer[ActorRef]) : Unit = {
    self ! DimensionMasterCreation(dim, slaves)
  }

  private def createWorkersRequest(dim : Int, slaves : ArrayBuffer[ActorRef]) : Unit = {
    for (thisSlave <- slaves){
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