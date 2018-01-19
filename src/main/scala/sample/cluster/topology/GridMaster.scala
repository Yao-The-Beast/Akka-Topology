package sample.cluster.topology

import akka.actor.{Actor, ActorRef, ActorSystem, Props, RootActorPath, Terminated}
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.{Cluster, Member}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ArrayBuffer

class GridMaster(
                  totalNodeMasters: Int,
                  gridConfig : GridConfig
                ) extends Actor with akka.actor.ActorLogging{

  var nodeMasters = Map[Int, ActorRef]()
  val cluster = Cluster(context.system)
  val registrationTimeout: FiniteDuration = 5.seconds
  //grid and master info
  //Dimension Number, NodeMaster Idx, [workers Indices]
  var dimMastersInfo = ArrayBuffer[(Int, Int, ArrayBuffer[Int])]()

  var totalReadyNodeMasters = 0;
  
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {

    case MemberUp(m) =>
      log.info(s"\n----Detect member ${m.address} up")
      //Step 1
      register(m)
      if (nodeMasters.size == totalNodeMasters) {
        println(s"----${nodeMasters.size} (out of ${totalNodeMasters}) node masters are up") 
        //Step 2       
        //generate grid info and masters for each dimensions
        generateDimMasters()
        //Step 3
        //propagate the info to all the node masters who are going to be (a) dimensional master 
        initNodeMasters()
      }

    case msg : NodeMasterReady =>
      totalReadyNodeMasters += 1
      //if all the nodeMasters are ready (all the children actors are online), kickstart the master-slave pairing  
      if (totalReadyNodeMasters == totalNodeMasters){
        for ((idx, nodeMaster) <- nodeMasters){
          nodeMaster ! StartNodeMaster()
        }
      }


    case Terminated(a) =>
      log.info(s"\n----$a is terminated, removing it from the set")
      for ((idx, nodeMaster) <- nodeMasters){
        if(nodeMaster == a) {
          nodeMasters -= idx
        }
      }
  }

  private def register(member: Member): Unit = {
    //member.getRoles().foreach(p => println(s"----p"))
    if (member.hasRole("NodeMaster")) {
      // awaiting here to prevent concurrent futures (from another message) trying to add to worker set at the same time
      val nodeMasterRef: ActorRef = Await.result(context.actorSelection(RootActorPath(member.address) / "user" / "NodeMaster").resolveOne(registrationTimeout), registrationTimeout + 1.second)
      context watch nodeMasterRef
      val newId: Integer = nodeMasters.size
      nodeMasters = nodeMasters.updated(newId, nodeMasterRef)
      log.info(s"\n----# of online node masters = ${nodeMasters.size}")
      //send a greeting msg to node master
      nodeMasterRef ! GreetingFromGridMaster(newId)
    }
  }

  private def initNodeMasters() : Unit = {
    for ((dim, nodeMasterIdx, slavesNodeMasterIdx) <- dimMastersInfo){
      var slavesMasterNodes = ArrayBuffer[ActorRef]()
      for (idx <- slavesNodeMasterIdx){
        slavesMasterNodes += nodeMasters(idx)
      }
      nodeMasters(nodeMasterIdx) ! DimensionMasterAssignment(dim, slavesMasterNodes)
    }
  }

  //Pre Generate a grid mapping
  //Assume our grid is 2 * 2
  // 0, 1
  // 2, 3
  // 0 is RM, 1 is CM, 2 is CM, 3 is RM
  private def generateDimMasters() : Unit = {
    dimMastersInfo += ((0, 0, ArrayBuffer(0,1)))
    dimMastersInfo += ((0, 3, ArrayBuffer(2,3)))
    dimMastersInfo += ((1, 1, ArrayBuffer(1,3)))
    dimMastersInfo += ((1, 2, ArrayBuffer(0,2)))
  }
}


case class GridConfig(totalNodeMasters: Int)

object GridMaster {
  def main(args: Array[String]): Unit = {
    
    val port = if (args.isEmpty) "2551" else args(0)
    val totalNodeMasters = if (args.length <= 1) 4 else args(1).toInt
    val gridConfig = GridConfig(totalNodeMasters = totalNodeMasters)

    initGridMaster(port, totalNodeMasters, gridConfig)
  }

  private def initGridMaster(port: String, totalNodeMasters: Int, gridConfig: GridConfig) = {
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [GridMaster]")).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ClusterSystem", config)

    system.log.info(s"-------\n Port = ${port} \n");
    system.actorOf(
      Props(
        classOf[GridMaster],
        totalNodeMasters,
        gridConfig
      ),
      name = "GridMaster"
    )
  }

  def startUp() = {
    main(Array())
  }

  def startUp(port: String, totalNodeMasters: Int, gridConfig: GridConfig): Unit = {
    initGridMaster(port: String, totalNodeMasters: Int, gridConfig: GridConfig)
  }

}
