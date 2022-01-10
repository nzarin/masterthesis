package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Gives an implementation for the routing table component of a kademlia node.
 */
public class RoutingTable implements Cloneable {

    /**
     * ID of the node that has this routing table.
     */
    public KadNode node = null;

    /**
     * K-Buckets of this node.
     */
    public TreeMap<Integer, KBucket> k_buckets = null;

    /**
     * Instantiates a new empty routing table with the specified size
     */
    public RoutingTable() {
        k_buckets = new TreeMap<Integer, KBucket>();
        // initialize k-bukets
        for (int i = 0; i <= KademliaCommonConfig.BITS; i++) {
            k_buckets.put(i, new KBucket());
        }
    }


    /**
     * Add a neighbour to the correct k-bucket.
     *
     * @param node The node to be added.
     */
    public void addNeighbour(KadNode node) {

        // get the length of the longest common prefix (correspond to the correct k-bucket)
        int prefix_len = Util.prefixLen(this.node.getNodeId(), node.getNodeId());

        // add the node to the corresponding k-bucket
        k_buckets.get(prefix_len).addNeighbour(node);
    }


    /**
     * Remove a neighbour from the correct k-bucket.
     *
     * @param node The node to be removed.
     */
    public void removeNeighbour(KadNode node) {

        // get the length of the longest common prefix (correspond to the correct k-bucket)
        int prefix_len = Util.prefixLen(this.node.getNodeId(), node.getNodeId());

        // add the node to the k-bucket
        k_buckets.get(prefix_len).removeNeighbour(node.getNodeId());
    }


    /**
     * Return the closest neighbour to a key from the correct k-bucket.
     *
     * @param searchkey The ID we are looking up
     * @param src       The original requester of this lookup
     * @return The k closest neighbors to the search key
     */
    public BigInteger[] getKClosestNeighbours(final KadNode searchkey, final KadNode src) {

        // resulting neighbors
        KadNode[] result = new KadNode[KademliaCommonConfig.K];

        // neighbour candidates
        ArrayList<KadNode> neighbour_candidates = new ArrayList<KadNode>();

        // get the length of the longest common prefix
        int prefix_len = Util.prefixLen(this.node.getNodeId(), searchkey.getNodeId());

        // return the k-bucket if it is full
        if (k_buckets.get(prefix_len).neighbours.size() >= KademliaCommonConfig.K) {
            return k_buckets.get(prefix_len).neighbours.keySet().toArray(result);
        }

        // else get k closest node from all k-buckets
        prefix_len = 0;
        while (prefix_len < KademliaCommonConfig.BITS) {
            neighbour_candidates.addAll(k_buckets.get(prefix_len).neighbours.keySet());

            prefix_len++;
        }

        // remove source id since it is the requester and cannot be in result
        neighbour_candidates.remove(src);

        // create a map (distance, node)
        TreeMap<BigInteger, BigInteger> distance_map = new TreeMap<BigInteger, BigInteger>();
        for (BigInteger node : neighbour_candidates) {
            distance_map.put(Util.distance(node, searchkey.getNodeId()), node);
        }

        // select the k closest nodes from the distance map
        int i = 0;
        for (BigInteger bi : distance_map.keySet()) {
            if (i < KademliaCommonConfig.K) {
                result[i] = distance_map.get(bi);
                i++;
            }
        }

        return result;
    }

    public Object clone() {
        RoutingTable dolly = new RoutingTable();
        for (int i = 0; i < KademliaCommonConfig.BITS; i++) {
            k_buckets.put(i, new KBucket());// (KBucket) k_buckets.get(i).clone());
        }
        return dolly;
    }

    /**
     * print a string representation of the table
     *
     * @return String
     */
    public String toString() {
        String s = "";
        for (Map.Entry<Integer, KBucket> entry : k_buckets.entrySet()) {
            String ith_k_bucket = entry.getValue().toString();
            s = s + "Nodes with common prefix " + entry.getKey() + " are : " + ith_k_bucket + "\n";
        }
        return s;
    }

}