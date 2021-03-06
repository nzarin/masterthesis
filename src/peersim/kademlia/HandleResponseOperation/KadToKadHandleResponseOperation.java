package peersim.kademlia.HandleResponseOperation;

import peersim.core.CommonState;
import peersim.kademlia.*;


/**
 * This class represents how the response should be handled when source and target are both KadNodes
 */
public class KadToKadHandleResponseOperation extends HandleResponseOperation2 {

    public KadToKadHandleResponseOperation(int kid, Message lookupMsg, int tid) {
        kademliaid = kid;
        lookupMessage = lookupMsg;
        transportid = tid;
        messageSender = new MessageSender(kademliaid, tid);
    }


    @Override
    public void handleResponse() {

        // remove the timer for the deadline because we already received a response on time
        lookupMessage.receiver.getSentMsgTracker().remove(lookupMessage.ackId);

        // add message sender to my routing table
        if (lookupMessage.sender != null && lookupMessage.sender instanceof KadNode) {
            lookupMessage.receiver.getRoutingTable().addNeighbour((KadNode) lookupMessage.sender);
        }

        // get corresponding find operation (using the message field operationId)
        FindOperation findOp = lookupMessage.receiver.getFindOperationsMap().get(lookupMessage.operationId);

        if (findOp != null) {

            //Step 1: update the closest set by saving the received neighbour
            try {
                findOp.updateClosestSet((KadNode[]) lookupMessage.body);
            } catch (Exception e) {
                findOp.available_requests++;
            }

            //update statistics of the find operation
            findOp.nrResponse++;

            //Periodically update statistics of the shortest amount of hops if we have found the target node for the first time. Otherwise, there will be another hop?
            if(findOp.nrResponse % KademliaCommonConfig.ALPHA == 0){

                //do we have found the message ?
                if(!findOp.getClosestSet().containsKey(findOp.destNode) && !findOp.alreadyFoundTarget){
                    findOp.shortestNrHops++;
                } else {
//                    System.err.println("We have found the target node! The shortestNrHops required was " + fop.shortestNrHops);
                    findOp.alreadyFoundTarget = true;
                }
            }

            // Step 2: send the new requests if allowed
            while (findOp.available_requests > 0) {
                KadNode neighbour = findOp.getNeighbour();

                //SCENARIO 1: there exists some neighbour we can visit.
                if (neighbour != null) {

                    //update statistics of the find operation
                    findOp.nrMessages++;

                    //create new request to send to this neighbour
                    Message request = new Message(Message.MSG_REQUEST);
                    request.src = lookupMessage.src;
                    request.target = lookupMessage.target;
                    request.sender = lookupMessage.receiver;
                    request.receiver = neighbour;
                    request.operationId = findOp.operationId;
                    request.newLookup = false;
//                    System.err.println("I am sending a REQUEST message to (" + request.receiver.getNodeId() + "," + request.receiver.getDomain() + ") of type " + request.receiver.getType() + " with msgId is " + request.msgId);

                    //send the ROUTE message
                    messageSender.sendMessage(request);

                    //SCENARIO 2: no new neighbour and no outstanding requests
                } else if (findOp.available_requests == KademliaCommonConfig.ALPHA) {
//                    System.err.println(" SCENARIO 2: NO NEW NEIGHBOUR AND NO OUTSTANDING REQUESTS");

                    // Search operation finished. The lookup terminates when the initiator has queried and gotten responses
                    // from the k closest nodes (from the closest set) it has seen.
                    lookupMessage.receiver.getFindOperationsMap().remove(findOp.operationId);

                    // if the lookup operation was not for bootstrapping purposes
                    if (findOp.body.equals("Automatically Generated Traffic")) {

                        // if the source of the lookup is the same as this receiver -> it was an intra-domain lookup
                        if (lookupMessage.src == lookupMessage.receiver){
                            Statistician.updateLookupStatistics((KadNode) lookupMessage.receiver, findOp, this.kademliaid);
                        } else {

                            // It is an inter-domain lookup. So first find the correct bridge node of the domain of the target node.
                            BridgeNode randomBridgeNodeThisDomain;
                            do{
                                randomBridgeNodeThisDomain = lookupMessage.receiver.getBridgeNodes().get(CommonState.r.nextInt(lookupMessage.receiver.getBridgeNodes().size()));
                            } while (randomBridgeNodeThisDomain == null);

                            //update statistics of the find operation
                            findOp.nrMessages++;

                            //create RESPONSE message to send it to the bridge node of this domain we got the request from
                            Message response = new Message(Message.MSG_RESPONSE);
                            response.body = findOp;
                            response.src = lookupMessage.src;
                            response.target = lookupMessage.target;
                            response.sender = lookupMessage.receiver;
                            response.receiver = randomBridgeNodeThisDomain;
                            response.operationId = lookupMessage.operationId;
//                            System.err.println("I am sending a RESPONSE message to (" + response.receiver.getNodeId() + "," +  response.receiver.getDomain() + ") of type " + response.receiver.getType() + " with msgId is " + response.msgId);
                            messageSender.sendMessage(response);
                        }

                    } else {
//                        System.err.println("This is a bootstrap message. Let it be. ");
                    }

                    return;

                    //SCENARIO 3: no neighbour available, but there are some open outstanding requests so just wait.
                } else {
//                    System.err.println("SCENARIO 3: NO NEW NEIGHBOUR BUT THERE ARE SOME OUTSTANDING REQUESTS ");
                    return;
                }
            }
        } else {
            System.err.println("There has been some error in the protocol");
        }


    }
}
