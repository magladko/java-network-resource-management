import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class AllocationRequest implements Callable<AllocationRequest> {

    private final Integer clientId;
    private Map<Character, Integer> resourcesToAllocate;
    private final NetworkNode node;
    //    private final ResourceManager manager;
    private List<String> protocolContent;
//    private Destination immediateRequestSource; // client if null
    private List<Destination> visitedNodes;
    private ServerSocket serverSocket;

    public AllocationRequest(Integer clientId, Map<Character, Integer> resourcesToAllocate,
                             List<String> protocolContent, NetworkNode node, List<Destination> visitedNodes) {
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.clientId = clientId;
        this.resourcesToAllocate = resourcesToAllocate;
        this.protocolContent = protocolContent;
        this.node = node;
        this.visitedNodes = visitedNodes;

//        this.protocolContent = isCompleted() ? "ALLOCATED" : clientId + " "
//                + resourcesToAllocate.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(" ", " ", ""))

//                resourcesToAllocate.entrySet().stream().map((e) -> e.getKey() + ":" + e.getValue() + " ").collect(Collectors.joining());

    }

    /**
     * Constructor used when request is directly from Client.
     *
     * @param in pattern (converted to String): "<identyfikator> <zasób>:<liczność> [<zasób>:liczność]"
     */
    public AllocationRequest(Integer clientId, List<String[]> in, NetworkNode node) {
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.clientId = clientId;
        this.node = node;
        this.visitedNodes = new ArrayList<>();
//        this.immediateRequestSource = null;

        protocolContent = new ArrayList<>();
        protocolContent.add("ALLOCATE " + node.getIp().getHostAddress() + ":" + node.getPort() + ":"
                                    + serverSocket.getLocalPort());
        protocolContent.add(String.join(" ", in.get(0)));

//        resourcesToAllocate = new HashMap<>();
        resourcesToAllocate = Arrays.stream(in.get(0)).skip(1).collect(Collectors.toMap(
                str -> str.split(":")[0].charAt(0),
                str -> Integer.parseInt(str.split(":")[1])
        ));

    }

    /**
     * Constructor used when request forwarded from NetworkNode.
     */
    public AllocationRequest(List<String[]> protocolMessage, NetworkNode node) {
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.clientId = Integer.parseInt(protocolMessage.get(1)[0]);
        this.resourcesToAllocate = Arrays.stream(protocolMessage.get(1))
                .skip(1)
                .collect(Collectors.toMap(
                        str -> str.split(":")[0].charAt(0),
                        str -> Integer.parseInt(str.split(":")[1])
                ));

        this.protocolContent = TCPHandler.convertInputListToStrList(protocolMessage);

        this.visitedNodes = new ArrayList<>();
        for (int i = 2; i < this.protocolContent.size(); i++) {
            try {
                this.visitedNodes.add(new Destination(
                        InetAddress.getByName(this.protocolContent.get(i).split(":")[2]),
                        Integer.parseInt(this.protocolContent.get(i).split(":")[3])
                ));

            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        this.node = node;
    }

//    public AllocationRequest() {}

    public Integer getClientId() {
        return clientId;
    }

    public List<String[]> getProtocolContentTab(String delim) {
        return protocolContent.stream().map(str -> str.split(delim)).collect(Collectors.toList());
    }

    public List<String> getProtocolContent() {
        return protocolContent;
    }

    public Boolean isCompleted() {
        return resourcesToAllocate.size() == 0;
    }

    public Map<Character, Integer> getResourcesToAllocate() {
        return resourcesToAllocate;
    }

    public NetworkNode getNode() {
        return node;
    }

//    public Destination getImmediateRequestSource() {
//        return immediateRequestSource;
//    }

    @Override
    public AllocationRequest call() throws Exception {

        // TODO CHECK IF EQUALS WORKS
        // if this NetworkNode was visited, just forward the request to where it has not been yet (or parent)
        if (this.visitedNodes.contains(node)){
            System.out.println(clientId + " node visited, forwarding...");
            manageRequestWhenThisNodeIsAlreadyChecked();
            System.out.println(clientId + " request forwarded");
        } else {

            // Node has not been visited

            final Boolean[] resourcesLocallyAllocated = new Boolean[1];

            // Try to allocate resources locally

            resourcesToAllocate.forEach((k, v) -> System.out.println(k + ":" + v));

            resourcesToAllocate.entrySet().removeIf(e -> {
                Integer allocatedAmount = node.getResourceManager()
                        .tryToAllocateResource(clientId, e.getKey(), e.getValue());

                protocolContent.add(
                        e.getKey() + ":" +
                                allocatedAmount + ":" +
                                node.getIp().getHostAddress() + ":" +
                                node.getPort() + ":" +
                                this.serverSocket.getLocalPort()
                );
                
                e.setValue(e.getValue() - allocatedAmount);
                
                resourcesLocallyAllocated[0] = allocatedAmount > 0;
                return e.getValue() == 0;
            });

            System.out.println("Allocated resources: " +
                    node.getResourceManager().getAllocatedResources().entrySet().stream().filter(
                    e -> Objects.equals(e.getKey().getKey(), clientId)).collect(Collectors.toList())
            );

            System.out.println(clientId + " isCompleted: " + isCompleted());

            if (isCompleted()) {
                // When request is completed, change protocol's header to ALLOCATED
                System.out.println(clientId + " request fully completed!");
                protocolContent.set(0, protocolContent.get(0).replaceFirst("ALLOCATE", "ALLOCATED"));

                // sending ALLOCATED status message to all waiting NetworkNodes
                Socket sendAllocatedStatusSocket;

                // sending ALLOCATED to ComNode
                Destination comNode = new Destination(
                        InetAddress.getByName(protocolContent.get(0).split(" ")[1].split(":")[0]),
                        Integer.parseInt(protocolContent.get(0).split(" ")[1].split(":")[2])
                );
                if (!node.equals(comNode)) {
                    sendAllocatedStatusSocket = new Socket(comNode.getIp(), comNode.getPort());
                    TCPHandler.sendMessage(getProtocolContentTab(" "), sendAllocatedStatusSocket);
                    sendAllocatedStatusSocket.close();
                }

                // sending ALLOCATED to every visited Node with allocated resources
                System.out.println(protocolContent.size() + "SIZEEEE");
                for (int i = 2; i < protocolContent.size(); i++) {
                    System.out.println(protocolContent.get(i).split(":")[2] + ":" +
                                               protocolContent.get(i).split(":")[4]);


                    Destination destination = new Destination(
                            InetAddress.getByName(protocolContent.get(i).split(":")[2]),
                            Integer.parseInt(protocolContent.get(i).split(":")[4]));

                    if (!visitedNodes.contains(destination) ||
                            Integer.parseInt(protocolContent.get(i).split(":")[1]) == 0 ||
                            destination.equals(comNode)) continue;

                    destination.setListenPort(Integer.parseInt(protocolContent.get(i).split(":")[4]));

                    sendAllocatedStatusSocket = new Socket(destination.getIp(), destination.getListenPort());

                    TCPHandler.sendMessage(getProtocolContentTab(" "), sendAllocatedStatusSocket);
                    sendAllocatedStatusSocket.close();
                }

            } else if (resourcesLocallyAllocated[0]) {
                // if allocated something, but not completed => forward the request and wait for response on listenPort

                for (Destination child : node.getChildrenNodes()) {
                    if (this.visitedNodes.contains(child)) continue;

                    child.forwardAllocationRequest(this);
                    // await child response
                    List<String[]> childResponse = TCPHandler.getMessage(serverSocket.accept());

                    if (Objects.equals(childResponse.get(0)[0], "FAILED")) {
                        node.getResourceManager().deallocate(Integer.parseInt(childResponse.get(1)[0]));
                    }
//                    else {
//                        manageRequestWhenThisNodeIsAlreadyChecked();
//                    }
                }
            } else {
                // if nothing allocated and not completed => forward the request and move on
                manageRequestWhenThisNodeIsAlreadyChecked();
            }
        }
        return this;    // OR PROTOCOL CONTENT ??
    }

    private void manageRequestWhenThisNodeIsAlreadyChecked() throws IOException {
        for (Destination child : node.getChildrenNodes()) {
            if (this.visitedNodes.contains(child)) continue;
            child.forwardAllocationRequest(this);
            return;
        }

        if (node.getParentNode() == null) {
            // allocation FAILED: deallocate ALL (also outer) resources, send info to ComNode
            node.getResourceManager().deallocate(clientId);

            Socket failedStatusSocket;

            // change protocol's header
            protocolContent.set(0, protocolContent.get(0).replaceFirst("ALLOCATED?", "FAILED"));

            // send FAILED status to ComNode
            failedStatusSocket = new Socket(InetAddress.getByName(getProtocolContentTab(" ").get(0)[1].split(":")[0]),
                                            Integer.parseInt(getProtocolContentTab(" ").get(0)[1].split(":")[2]));
            TCPHandler.sendMessage(getProtocolContentTab(" "), failedStatusSocket);
            failedStatusSocket.close();


            // send FAILED status to Nodes which allocated resources using listenPort
            for (int i = 2; i < protocolContent.size(); i++) {
                String[] lineParams = getProtocolContentTab(":").get(i);
                if (Integer.parseInt(lineParams[1]) != 0) {
                    failedStatusSocket = new Socket(InetAddress.getByName(lineParams[2]), Integer.parseInt(lineParams[4]));
                    TCPHandler.sendMessage(getProtocolContentTab(" "), failedStatusSocket);
                    failedStatusSocket.close();
                }
            }

        } else node.getParentNode().forwardAllocationRequest(this);
    }
}
