package peersim.kademlia;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;
import peersim.edsim.EDSimulator;

import java.util.Comparator;

/**
 * Turbulent class is only for test/statistical purpose. This Control execute a node add or remove (failure) with a given
 * probability.
 * The probabilities are configurable from the parameters p_idle, p_add, p_rem.
 * - p_idle (default = 0): probability that the current execution does nothing (i.e. no adding and no failures).
 * - p_add (default = 0.5): probability that a new node is added in this execution.
 * - p_rem (default = 0.5): probability that this execution will result in a failure of an existing node.
 * If the user desire to change one probability, all the probability value MUST be indicated in the configuration file.
 * Other parameters:
 * - maxsize (default: infinite): max size of network. If this value is reached no more add operation are performed.
 * - minsize (default: 1): min size of network. If this value is reached no more remove operation are performed.
 */

public class Turbulence implements Control {

    private static final String PAR_PROT = "protocol";
    private static final String PAR_TRANSPORT = "transport";
    private static final String PAR_INIT = "init";

    /**
     * Specify a minimum size for the network. By default there is no limit.
     */
    private static final String PAR_MINSIZE = "minsize";

    /**
     * Specify a maximum size for the network. By default there is limit of 1.
     */
    private static final String PAR_MAXSIZE = "maxsize";

    /**
     * Idle probability
     */
    private static final String PAR_IDLE = "p_idle";

    /**
     * Probability to add a node (in non-idle execution)
     */
    private static final String PAR_ADD = "p_add";

    /**
     * Probability to fail a node (in non-idle execution). Note: nodes will NOT be removed from the network, but will be set as
     * "DOWN", in order to let peersim exclude automatically the delivering of events destinates to them.
     */
    private static final String PAR_REM = "p_rem";

    /**
     * Node initializers to apply on the newly added nodes
     */
    protected NodeInitializer[] inits;

    private final String prefix;
    private final int kademliaid;
    private final int transportid;
    private final int maxsize;
    private final int minsize;
    private final double p_idle;
    private final double p_add;
    private final double p_rem;

    /**
     * Initiate turbulence in the system.
     *
     * @param prefix
     */
    public Turbulence(String prefix) {
        this.prefix = prefix;
        kademliaid = Configuration.getPid(this.prefix + "." + PAR_PROT);
        transportid = Configuration.getPid(this.prefix + "." + PAR_TRANSPORT);

        minsize = Configuration.getInt(this.prefix + "." + PAR_MINSIZE, 1);
        maxsize = Configuration.getInt(this.prefix + "." + PAR_MAXSIZE, Integer.MAX_VALUE);

        Object[] tmp = Configuration.getInstanceArray(prefix + "." + PAR_INIT);
        inits = new NodeInitializer[tmp.length];
        for (int i = 0; i < tmp.length; ++i)
            inits[i] = (NodeInitializer) tmp[i];

        // load probability from configuration file
        p_idle = Configuration.getDouble(this.prefix + "." + PAR_IDLE, 0); // idle default 0
        p_add = Configuration.getDouble(this.prefix + "." + PAR_ADD, 0.5); // add default 0.5
        p_rem = Configuration.getDouble(this.prefix + "." + PAR_REM, 0.5); // add default 0.5

        // check probability values
        if (p_idle < 0 || p_idle > 1) {
            System.err.println("Wrong event probability in Turbulence class: the probability PAR_IDLE must be between 0 and 1");
        } else if (p_add < 0 || p_add > 1) {
            System.err.println("Wrong event probability in Turbulence class: the probability PAR_ADD must be between 0 and 1");
        } else if (p_rem < 0 || p_rem > 1) {
            System.err.println("Wrong event probability in Turbulence class: the probability PAR_REM must be between 0 and 1");
        } else if (p_idle + p_add + p_idle > 1) {
            System.err.println("Wrong event probability in Turbulence class: the sum of PAR_IDLE, PAR_ADD and PAR_REM must be 1");
        }

        System.err.println(String.format("Turbulence: [p_idle=%f] [p_add=%f] [p_remove=%f] [(min,max)=(%d,%d)]", p_idle, p_add, p_rem, minsize, maxsize));
    }

    @SuppressWarnings("unchecked")
    public void sortNet() {
        Network.sort(new Comparator() {
            // ______________________________________________________________________________________
            public int compare(Object o1, Object o2) {
                Node n1 = (Node) o1;
                Node n2 = (Node) o2;
                KademliaProtocol p1 = (KademliaProtocol) (n1.getProtocol(kademliaid));
                KademliaProtocol p2 = (KademliaProtocol) (n2.getProtocol(kademliaid));
                return Util.put0(p1.getCurrentNode().getNodeId()).compareTo(Util.put0(p2.getCurrentNode().getNodeId()));
            }

            public boolean equals(Object obj) {
                return compare(this, obj) == 0;
            }
        });
    }

    /**
     * Add a new node randomly while the protocol is running.
     *
     * @return Always false.
     */
    public boolean add() {

        // Add node to network
        Node newNetworkNode = (Node) Network.prototype.clone();
        for (int j = 0; j < inits.length; ++j)
            inits[j].initialize(newNetworkNode);
        Network.add(newNetworkNode);

        // create new kademlia node
        UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS,  CommonState.r);
        KadNode newKadNode = new KadNode(urg.generateID(), urg.selectDomain());
        ((KademliaProtocol) (newNetworkNode.getProtocol(kademliaid))).setKadNode(newKadNode);
        newKadNode.getRoutingTable().setOwnerKadNode(newKadNode);

        System.err.println("New node is spawn with node id : " + newKadNode.getNodeId() + " in domain " + newKadNode.getDomain());

        // sort network
        sortNet();

        // find random node to add to k-bucket
        Node bootstrapNode = selectBootstrapNode(newKadNode);

        // create auto-search message (search message with destination my own Id)
        Message m = Message.makeEmptyMessage("Bootstrap traffic", Message.MSG_FINDNODE);
        m.timestamp = CommonState.getTime();
        m.dest = newKadNode;

        // perform initialization
        newKadNode.getRoutingTable().addNeighbour((KadNode) ((KademliaProtocol) bootstrapNode.getProtocol(kademliaid)).getCurrentNode());

        // start auto-search
        EDSimulator.add(0, m, newNetworkNode, kademliaid);

//        // find another random node (this is to enrich the k-buckets)
//        Node bootstrapNode2 = selectBootstrapNode(newKadNode);
//
//        Message m1 = Message.makeEmptyMessage("Bootstrap traffic", Message.MSG_FINDNODE);
//        m1.timestamp = CommonState.getTime();
//        m1.dest = urg.generateID();

        return false;
    }


    private Node selectBootstrapNode(KadNode newKadNode){
        // select one random bootstrap node within this domain
        Node bootstrapNode;
        KademliaProtocol kadP;
        KadNode bootstrapKadNode = null;
        do {
            bootstrapNode = Network.get(CommonState.r.nextInt(Network.size()));
            kadP = (KademliaProtocol) (bootstrapNode.getProtocol(kademliaid));
            KadNode temp = (KadNode) kadP.getCurrentNode();

            //only nodes that are in the same domain can be bootstrap nodes
            if(temp.getDomain() == newKadNode.getDomain()){
                bootstrapKadNode = temp;
            }

        } while ((bootstrapNode == null) || (bootstrapKadNode == null) || (!bootstrapNode.isUp()));

        return bootstrapNode;
    }

    /**
     * Select a random node to be 'removed'.
     *
     * @return Always false.
     */
    public boolean rem() {
        Node remove;
        do {
            remove = Network.get(CommonState.r.nextInt(Network.size()));
        } while ((remove == null) || (!remove.isUp()));

        // remove node (set its state to DOWN)
        remove.setFailState(Node.DOWN);

        return false;
    }

    /**
     * Every call of this control adds or removes a node from the network.
     *
     * @return Always false.
     */
    public boolean execute() {
        // throw the dice
        double dice = CommonState.r.nextDouble();
        if (dice < p_idle)
            return false;

        // update network size
        int sz = Network.size();
        for (int i = 0; i < Network.size(); i++)
            if (!Network.get(i).isUp())
                sz--;

        // perform the correct operation based on the probability
        if (dice < p_idle) {
            return false; // do nothing
        } else if (dice < (p_idle + p_add) && sz < maxsize) {
            return add();
        } else if (sz > minsize) {
            return rem();
        }

        return false;
    }

}

