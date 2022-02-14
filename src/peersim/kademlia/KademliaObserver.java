package peersim.kademlia;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalStats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class implements a simple observer of search time and hop average in finding a node in the network
 */
public class KademliaObserver implements Control {

    /**
     * Parameter of the protocol we want to observe
     */
    private static final String PAR_PROT = "protocol";

    // OVERALL statistics
    public static IncrementalStats find_op_OVERALL = new IncrementalStats();
    public static IncrementalStats hopStore_OVERALL = new IncrementalStats();
    public static IncrementalStats messageStore_OVERALL = new IncrementalStats();
    public static IncrementalStats timeStore_OVERALL = new IncrementalStats();
    public static IncrementalStats shortestAmountHops_OVERALL = new IncrementalStats();
    public static IncrementalStats finished_lookups_OVERALL = new IncrementalStats();
    public static IncrementalStats failed_lookups_OVERALL = new IncrementalStats();
    public static IncrementalStats successful_lookups_OVERALL = new IncrementalStats();
    public static IncrementalStats fraction_f_CS_OVERALL = new IncrementalStats();


    // INTRA-DOMAIN LOOKUP statistics
    public static IncrementalStats hopStore_INTRA = new IncrementalStats();
    public static IncrementalStats messageStore_INTRA = new IncrementalStats();
    public static IncrementalStats shortestAmountHops_INTRA = new IncrementalStats();
    public static IncrementalStats timeStore_INTRA = new IncrementalStats();
    public static IncrementalStats finished_lookups_INTRA = new IncrementalStats();
    public static IncrementalStats failed_lookups_INTRA = new IncrementalStats();
    public static IncrementalStats successful_lookups_INTRA = new IncrementalStats();
    public static IncrementalStats fraction_f_CS_INTRA = new IncrementalStats();

    // INTER-DOMAIN LOOKUP statistics
    public static IncrementalStats hopStore_INTER = new IncrementalStats();
    public static IncrementalStats messageStore_INTER = new IncrementalStats();
    public static IncrementalStats shortestAmountHops_INTER = new IncrementalStats();
    public static IncrementalStats timeStore_INTER = new IncrementalStats();
    public static IncrementalStats finished_lookups_INTER = new IncrementalStats();
    public static IncrementalStats failed_lookups_INTER = new IncrementalStats();
    public static IncrementalStats successful_lookups_INTER = new IncrementalStats();
    public static IncrementalStats fraction_f_CS_INTER = new IncrementalStats();

    /**
     * Protocol id
     */
    private final int pid;

    /**
     * Prefix to be printed in output
     */
    private final String prefix;

    /**
     * Constructor that links the Control and Protocol objects.
     *
     * @param prefix
     */
    public KademliaObserver(String prefix) {
        this.prefix = prefix;
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
    }

    /**
     * Print the statistical snapshot of the current situation
     *
     * @return boolean always false
     */
    public boolean execute() {
        // get the real network size
        int sz = Network.size();

        //update the network size such that it contains only reachable nodes
        for (int i = 0; i < Network.size(); i++)
            if (!Network.get(i).isUp())
                sz--;

        // calculate overall success ratio
        double success_lookups = successful_lookups_OVERALL.getSum();
        double failure_lookups = failed_lookups_OVERALL.getSum();
        double no_btstrp_completed_lookups = success_lookups + failure_lookups;
        double success_ratio = success_lookups / no_btstrp_completed_lookups;

        //calculate intra-domain success ratio
        double success_lookups_intra = successful_lookups_INTRA.getSum();
        double failure_lookups_intra = failed_lookups_INTRA.getSum();
        double completed_lookups_intra = success_lookups_intra + failure_lookups_intra;
        double success_ratio_intra = success_lookups_intra / completed_lookups_intra;

        //calculate inter-domain success ratio
        double success_lookups_inter = successful_lookups_INTER.getSum();
        double failure_lookups_inter = failed_lookups_INTER.getSum();
        double completed_lookups_inter = success_lookups_inter + failure_lookups_inter;
        double success_ratio_inter = success_lookups_inter / completed_lookups_inter;



        //format print result
        String s = String.format("[time=%d]:[N=%d current nodes UP]  [%d min ltcy] [%d msec average ltcy] [%d max ltcy]  [%f completed findops] [%f success lookups] [%f failed lookups]  [%f success ratio] [%f shortest amount of hops] [%f INTRA-DOMAIN lookups] [%f INTER-DOMAIN lookups]",
                CommonState.getTime(), sz, (int) timeStore_OVERALL.getMin(), (int) timeStore_OVERALL.getAverage(), (int) timeStore_OVERALL.getMax(), no_btstrp_completed_lookups, success_lookups, failure_lookups, success_ratio, shortestAmountHops_OVERALL.getAverage(), finished_lookups_INTRA.getSum(), finished_lookups_INTER.getSum());

        // create files
        try {

            //create OVERALL files
            File hop_file_OVERALL = new File("results/hops/shortestHops-OVERALL.txt");
            hop_file_OVERALL.createNewFile();
            BufferedWriter outH_OVERALL = new BufferedWriter(new FileWriter(hop_file_OVERALL, false));
            outH_OVERALL.write(shortestAmountHops_OVERALL.toString() + "\n");
            outH_OVERALL.close();

            File latency_file_OVERALL = new File("results/latency/avgLatency-OVERALL.txt");
            latency_file_OVERALL.createNewFile();
            BufferedWriter outL_OVERALL = new BufferedWriter(new FileWriter(latency_file_OVERALL, false));
            outL_OVERALL.write(timeStore_OVERALL.toString() + "\n");
            outL_OVERALL.close();

            File success_file_OVERALL = new File("results/successratio/avgSR-OVERALL.txt");
            success_file_OVERALL.createNewFile();
            BufferedWriter outS_OVERALL = new BufferedWriter(new FileWriter(success_file_OVERALL, false));
            outS_OVERALL.write(success_ratio + "\n");
            outS_OVERALL.close();

            File message_file_OVERALL = new File("results/messages/avgMessages-OVERALL.txt");
            message_file_OVERALL.createNewFile();
            BufferedWriter outM_OVERALL = new BufferedWriter(new FileWriter(message_file_OVERALL, false));
            outM_OVERALL.write(messageStore_OVERALL.toString() + "\n");
            outM_OVERALL.close();

            File adversarial_file_OVERALL = new File("results/adversarials/fraction-f-OVERALL.txt");
            adversarial_file_OVERALL.createNewFile();
            BufferedWriter outA_OVERALL = new BufferedWriter(new FileWriter(adversarial_file_OVERALL, false));
            outA_OVERALL.write(fraction_f_CS_OVERALL.toString() + "\n");
            outA_OVERALL.close();

            //create INTRA-DOMAIN LOOKUP files
            File hop_file_INTRA = new File("results/hops/shortestHops-INTRA.txt");
            hop_file_INTRA.createNewFile();
            BufferedWriter outH_INTRA = new BufferedWriter(new FileWriter(hop_file_INTRA, false));
            outH_INTRA.write(shortestAmountHops_INTRA.toString() + "\n");
            outH_INTRA.close();

            File latency_file_INTRA = new File("results/latency/avgLatency-INTRA.txt");
            latency_file_INTRA.createNewFile();
            BufferedWriter outL_INTRA = new BufferedWriter(new FileWriter(latency_file_INTRA, false));
            outL_INTRA.write(timeStore_INTRA.toString() + "\n");
            outL_INTRA.close();

            File success_file_INTRA = new File("results/successratio/avgSR-INTRA.txt");
            success_file_INTRA.createNewFile();
            BufferedWriter outS_INTRA = new BufferedWriter(new FileWriter(success_file_INTRA, false));
            outS_INTRA.write(success_ratio_intra + "\n");
            outS_INTRA.close();

            File message_file_INTRA = new File("results/messages/avgMessages-INTRA.txt");
            message_file_INTRA.createNewFile();
            BufferedWriter outM_INTRA = new BufferedWriter(new FileWriter(message_file_INTRA, false));
            outM_INTRA.write(messageStore_INTRA.toString() + "\n");
            outM_INTRA.close();

            File adversarial_file_INTRA = new File("results/adversarials/fraction-f-INTRA.txt");
            adversarial_file_INTRA.createNewFile();
            BufferedWriter outA_INTRA = new BufferedWriter(new FileWriter(adversarial_file_INTRA, false));
            outA_INTRA.write(fraction_f_CS_INTRA.toString() + "\n");
            outA_INTRA.close();

            //create INTER-DOMAIN LOOKUP files
            File hop_file_INTER = new File("results/hops/shortestHops-INTER.txt");
            hop_file_INTER.createNewFile();
            BufferedWriter outH_INTER= new BufferedWriter(new FileWriter(hop_file_INTER, false));
            outH_INTER.write(shortestAmountHops_INTER.toString() + "\n");
            outH_INTER.close();

            File latency_file_INTER = new File("results/latency/avgLatency-INTER.txt");
            latency_file_INTER.createNewFile();
            BufferedWriter outL_INTER = new BufferedWriter(new FileWriter(latency_file_INTER, false));
            outL_INTER.write(timeStore_INTER.toString() + "\n");
            outL_INTER.close();

            File success_file_INTER = new File("results/successratio/avgSR-INTER.txt");
            success_file_INTER.createNewFile();
            BufferedWriter outS_INTER = new BufferedWriter(new FileWriter(success_file_INTER, false));
            outS_INTER.write(success_ratio_inter + "\n");
            outS_INTER.close();

            File message_file_INTER = new File("results/messages/avgMessages-INTER.txt");
            message_file_INTER.createNewFile();
            BufferedWriter outM_INTER = new BufferedWriter(new FileWriter(message_file_INTER, false));
            outM_INTER.write(messageStore_INTER.toString() + "\n");
            outM_INTER.close();

            File adversarial_file_INTER = new File("results/adversarials/fraction-f-INTER.txt");
            adversarial_file_INTER.createNewFile();
            BufferedWriter outA_INTER = new BufferedWriter(new FileWriter(adversarial_file_INTER, false));
            outA_INTER.write(fraction_f_CS_INTER.toString() + "\n");
            outA_INTER.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println();
        System.err.println(s);

        return false;
    }
}
