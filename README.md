Code lies in "src/main/cluster/sample/cluster/topology/"

Run GridMaster: sbt "runMain sample.cluster.topology.GridMaster" (Need one GridMaster)

Run NodeMaster: sbt "sbt "runMain sample.cluster.topology.NodeMaster" (Need four NodeMasters)

Important!
Current configuration is hardcoded (including grid formation and master assignment).
The grid is 2 * 2. Each node is either a row / column master. 

GridMaster is the root with 4 Node Masters as its children.
Each NodeMaster will spawn 2 workers and 1 dimension (Row/column) master. 
So there are three levels.

Logic Flow:
GridMaster talks to the 4 NodeMasters about whethere they need to spawn (a) dimensionMaster (child actor).
If the nodeMaster has to spawn dimensionMasters, it will also be told the addresses of other NodeMasters who are going to be its slaves.

Besides, the nodeMaster is also going to inform other nodeMasters if they need to spawn workers that correpsonds to different dimensions.  
Eg. If NM1 creates a DM1 that is responsible for dimension 1 and DM1's slaves are node1, node2, NM1 will inform NM1 and NM2 to create two children actors (Worker1 on node1, Worker1 on Node2) that are reponsible for dimension1. 

After all the children actors are created (including both DM and Workers) by NM, NM will inform GM that NM is ready. After GM are aware that all NMs are ready, GM will issue commands to NMs to let their DMs locate their slaves. 

DMs will issue query to NMs asking for the actorRef of the slaves. After DMs have all the slaves references, it will broadcast the addresses of the workers to each individual workers within the group. The rest will be identical to the AllReduce.
