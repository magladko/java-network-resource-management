import java.util.List;
import java.util.Set;

public class NetworkNode {

    private Integer id;
    private Integer clientConnectionPort;
    private NetworkNode parentNode;
    private List<NetworkNode> childrenNodes;
    private Set<Resource> resources;

    public NetworkNode(Integer id, Integer clientConnectionPort, NetworkNode parentNode,
                       List<NetworkNode> childrenNodes, Set<Resource> resources) {
        this.id = id;
        this.clientConnectionPort = clientConnectionPort;
        this.parentNode = parentNode;
        this.childrenNodes = childrenNodes;
        this.resources = resources;
    }

    public static void main(String[] args) {
        // parameter storage
        int ident = -1;
        int tcpPort = -1;
        String gateway = null;
        String resourceList = null;

        // Parameter scan loop
        for(int i=0; i<args.length; i++) {
            switch (args[i]) {
                case "-ident":
                    ident = Integer.parseInt(args[++i]);
                    break;
                case "-tcpport":
                    tcpPort = Integer.parseInt(args[++i]);
                case "-gateway":
                    String[] gatewayArray = args[++i].split(":");
                    gateway = gatewayArray[0];
                    tcpPort = Integer.parseInt(gatewayArray[1]);
                    break;
                default:
                    if(resourceList == null) resourceList = args[i];
                    else if(! "TERMINATE".equals(resourceList)) resourceList += " " + args[i];
            }
        }
    }
}
