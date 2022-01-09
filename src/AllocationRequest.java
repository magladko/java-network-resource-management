import sun.nio.ch.Net;

import javax.swing.plaf.IconUIResource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AllocationRequest implements Callable<AllocationRequest> {

    private Integer clientId;
    private AllocationStatus allocationStatus;
    private Map<Character, Integer> resourcesToAllocate;

    private Destination comNode;
    private List<RequestAllocationInfo> allocationHistory;

    private final NetworkNode node;

    private ServerSocket serverSocket;
    private Boolean forwardedWithoutWaiting;

    public AllocationRequest(AllocationStatus allocationStatus, Destination comNode, Integer clientId,
                             Map<Character, Integer> resourcesToAllocate,
                             List<RequestAllocationInfo> allocationHistory, NetworkNode node) {
        this.allocationStatus = allocationStatus;
        this.comNode = comNode;
        this.clientId = clientId;
        this.resourcesToAllocate = resourcesToAllocate;
        this.allocationHistory = allocationHistory;
        this.node = node;
    }

    /**
     * Constructor used when request is directly from Client.
     *
     * @param in pattern (converted to String): "<identyfikator> <zasób>:<liczność> [<zasób>:liczność]"
     */
    public AllocationRequest(Integer clientId, List<String[]> in, NetworkNode node) throws IOException {
        if (NetworkNode.DEBUG_INFO) System.out.println("from client CONSTRUCTOR initiated");

        serverSocket = new ServerSocket(0);

        this.comNode = node;

        this.allocationStatus = AllocationStatus.ALLOCATE;
        forwardedWithoutWaiting = false;

        this.clientId = clientId;
        this.node = node;
        this.node.setListenPort(serverSocket.getLocalPort());
        this.allocationHistory = new ArrayList<>();

        resourcesToAllocate = Arrays.stream(in.get(0)).skip(1).collect(Collectors.toMap(
                str -> str.split(":")[0].charAt(0),
                str -> Integer.parseInt(str.split(":")[1])
        ));
        if (NetworkNode.DEBUG_INFO) System.out.println("from client CONSTRUCTOR finished");
    }

    /**
     * Constructor used when request forwarded from NetworkNode.
     */
    public AllocationRequest(List<String[]> protocolMessage, NetworkNode node) throws IOException {
        if (NetworkNode.DEBUG_INFO) System.out.println("from NetNode CONSTRUCTOR initiated");
        this.node = node;
        allocationHistory = new ArrayList<>();

//        if (NetworkNode.DEBUG_INFO) System.out.println("constructor started...");

        // LINE 1 => ALLOCATE <ComNodeIP>:<ComNodePORT>:<listenPort>    // OR
        // LINE 1 => ALLOCATED                                          // OR
        // LINE 1 => FAILED
        allocationStatus = AllocationStatus.valueOf(protocolMessage.get(0)[0]);

        // RESET LISTENING PORTS
        if (this.node.getParentNode() != null) {
            this.node.getParentNode().setListenPort(null);
        }
        for (int j = 0; j < this.node.getChildrenNodes().size(); j++) {
            this.node.getChildrenNodes().get(j).setListenPort(null);
        }

//        int temp = 1;
//        if (NetworkNode.DEBUG_INFO) System.out.println(temp++);

        if (allocationStatus.equals(AllocationStatus.ALLOCATE)) {
            // LINE 1 => ALLOCATE <ComNodeIP>:<ComNodePORT>:<listenPort>
            comNode = new Destination(
                    InetAddress.getByName(protocolMessage.get(0)[1].split(":")[0]),
                    Integer.parseInt(protocolMessage.get(0)[1].split(":")[1]),
                    Integer.parseInt(protocolMessage.get(0)[1].split(":")[2])
            );

//            if (NetworkNode.DEBUG_INFO) System.out.println(temp++);

            // LINE 2 => <clientId> <zasób>:<liczność> [<zasób>:liczność]
            clientId = Integer.parseInt(protocolMessage.get(1)[0]);
            resourcesToAllocate = Arrays.stream(protocolMessage.get(1)).skip(1).collect(Collectors.toMap(
                    str -> str.split(":")[0].charAt(0),
                    str -> Integer.parseInt(str.split(":")[1])
            ));

//            if (NetworkNode.DEBUG_INFO) System.out.println(temp++);



            // LINE 3... => <zasób>:<liczność>:<ip węzła>:<port węzła>[:<listenPort>]

            for (int i = 2; i < protocolMessage.size(); i++) {
                String[] infoLine = protocolMessage.get(i)[0].split(":");
                if (infoLine.length < 4) break;
                Destination d = new Destination(
                        InetAddress.getByName(infoLine[2]),
                        Integer.parseInt(infoLine[3]),
                        infoLine.length == 5 ? Integer.parseInt(infoLine[4]) : null);

                if (this.node.getParentNode() != null) {
                    if (d.equals(this.node.getParentNode())) this.node.setParentNode(d);
//                    else this.node.getParentNode().setListenPort(null);
                }
                for (int j = 0; j < this.node.getChildrenNodes().size(); j++) {
//                    System.out.println(" AAA" + j);
                    if (this.node.getChildrenNodes().get(j).equals(d)) {
                        this.node.getChildrenNodes().set(j, d);
                    }
                }

//                else if (node.getChildrenNodes().contains(d)) {
//
//                    node.getChildrenNodes().forEach(destination -> {
//                        if (d.equals(destination)) {
//                            destination.setListenPort(d.getListenPort());
//                        }
//                    });
////                    node.getChildrenNodes().stream().filter(d::equals).map(destination -> d);
//                }

//                if (NetworkNode.DEBUG_INFO) System.out.println(Arrays.toString(infoLine));
                allocationHistory.add(new RequestAllocationInfo(
                        infoLine[0].charAt(0),
                        Integer.parseInt(infoLine[1]),
                        d
                ));

            }
        } else {
            if (allocationStatus.equals(AllocationStatus.ALLOCATED)) {
                // LINE 2... => <zasób>:<liczność>:<ip węzła>:<port węzła>

                for (int i = 1; i < protocolMessage.size(); i++) {
                    String[] infoLine = protocolMessage.get(i)[0].split(":");
                    if (infoLine.length < 4) break;

                    if (Integer.parseInt(infoLine[1]) == 0) continue;
                    Destination d = new Destination(
                            InetAddress.getByName(infoLine[2]),
                            Integer.parseInt(infoLine[3]),
                            infoLine.length == 5 ? Integer.parseInt(infoLine[4]) : null);

//                    if (d.getListenPort() != null && d.equals(node.getParentNode())) {
//                        node.getParentNode().setListenPort(d.getListenPort());
//                    }


                    if (this.node.getParentNode() != null) {
                        if (d.equals(this.node.getParentNode())) this.node.setParentNode(d);
//                        else this.node.getParentNode().setListenPort(null);
                    }
                    for (int j = 0; j < this.node.getChildrenNodes().size(); j++) {
//                        this.node.getChildrenNodes().get(j).setListenPort(null);
                        if (this.node.getChildrenNodes().get(j).equals(d)) {
                            this.node.getChildrenNodes().set(j, d);
                        }
                    }


//                    else if (node.getChildrenNodes().contains(d)) {
//                        node.getChildrenNodes().forEach(destination -> {
//                            if (d.equals(destination)) {
//                                destination.setListenPort(d.getListenPort());
//                            }
//                        });
//                    node.getChildrenNodes().stream().filter(d::equals).map(destination -> d);
//                    }


                    allocationHistory.add(new RequestAllocationInfo(
                            infoLine[0].charAt(0),
                            Integer.parseInt(infoLine[1]),
                            d
                    ));
                }
            } else this.clientId = Integer.parseInt(protocolMessage.get(1)[0]);


//            if (NetworkNode.DEBUG_INFO) System.out.println(temp++);
        }
//        if (NetworkNode.DEBUG_INFO) System.out.println("constructor finished...");

//        node.getParentNode().setListenPort(allocationHistory.stream().map(RequestAllocationInfo::getLocation)
//                                                   .filter(dest -> dest.) .findAny().orElse(null).getListenPort());

//        if (allocationHistory.stream().map(RequestAllocationInfo::getLocation).findAny().isPresent()) {
//            node.getParentNode()
//        }
        if (NetworkNode.DEBUG_INFO) {
            if (this.node.getParentNode() != null) System.out.println("parentNode: " + this.node.getParentNode().getStringForProtocol());
            System.out.println("children: ");
            this.node.getChildrenNodes().forEach(destination -> System.out.print(destination.getStringForProtocol() + " ; "));
            System.out.println();
        }

        if (NetworkNode.DEBUG_INFO) System.out.println("from NetNode CONSTRUCTOR finished");
    }

    public Integer getClientId() {
        return clientId;
    }

    public Boolean isCompleted() {
        return resourcesToAllocate == null || resourcesToAllocate.size() == 0;
    }

    public Map<Character, Integer> getResourcesToAllocate() {
        return resourcesToAllocate;
    }

    public NetworkNode getNode() {
        return node;
    }

    public Destination getComNode() {
        return comNode;
    }

    public AllocationStatus getAllocationStatus() {
        return allocationStatus;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public List<Destination> getVisitedNodes() {
        List<Destination> visitedNodes = new ArrayList<>();
        allocationHistory.forEach(requestAllocationInfo ->
            visitedNodes.add(requestAllocationInfo.getLocation())
        );

        return visitedNodes;
    }

    public List<RequestAllocationInfo> getAllocationHistory() {
        return allocationHistory;
    }

    public Boolean getForwardedWithoutWaiting() {
        return forwardedWithoutWaiting;
    }

    /**
     * Allocation Request protocol types:
     *
     * @param toClient = false
     * ALLOCATE <ComNodeIP>:<ComNodePORT>:<listenPort>
     * <clientId> <zasób>:<liczność> [<zasób>:liczność]
     * [<zasób>:<liczność>:<ip węzła>:<port węzła>[:<listenPort>]]
     * [<zasób>:<liczność>:<ip węzła>:<port węzła>[:<listenPort>]]
     * [...]
     * // end with empty line
     *
     * ALLOCATED
     * <zasób>:<liczność>:<ip węzła>:<port węzła>
     * [<zasób>:<liczność>:<ip węzła>:<port węzła>]
     * [...]
     *
     * FAILED
     * <clientId>
     *
     * =================================
     * @param toClient = true
     * ALLOCATED
     * <zasób>:<liczność>:<ip węzła>:<port węzła>
     * [<zasób>:<liczność>:<ip węzła>:<port węzła>]
     * [...]
     *
     * FAILED
     *
     */
    public String buildProtocol(Boolean toClient) {
        if (toClient && allocationStatus.equals(AllocationStatus.ALLOCATE))
            throw new IllegalArgumentException();

        String result = allocationStatus.getStatusString();
        switch (allocationStatus) {
            case ALLOCATE:
                if (comNode != null) result += " " + comNode.getStringForProtocol();
                result += "\n";
                result += clientId + " ";
                
                // assures that there are no redundant requests left
                resourcesToAllocate.entrySet().removeIf(e -> e.getValue() == 0);


                StringBuilder resultBuilder = new StringBuilder(result);
                for (Map.Entry<Character, Integer> entry : resourcesToAllocate.entrySet()) {
                    resultBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
                }
                result = resultBuilder.toString();
                result = result.substring(0, result.length() - 1);
                result += "\n";

                StringBuilder resultBuilder1 = new StringBuilder(result);
                for (RequestAllocationInfo info : allocationHistory) {
                    resultBuilder1.append(info.toString()).append("\n");
                }
                result = String.valueOf(resultBuilder1);
                result += "\n";
                break;
            case ALLOCATED:
                result += "\n";
//                if (!toClient) result += clientId + "\n";
//                else {
                resultBuilder1 = new StringBuilder(result);
                for (RequestAllocationInfo info : allocationHistory) {
                    if (info.getAmount() == 0) continue;
                    resultBuilder1.append(info.toStringNoListenPort()).append("\n");
                }
                result = String.valueOf(resultBuilder1);
//                }
                break;
            case FAILED:
                result += "\n";
                if (!toClient) result += clientId + "\n";
                break;
            default:
                throw new EnumConstantNotPresentException(AllocationStatus.class, allocationStatus.name());
        }
        return result;
    }

    @Override
    public AllocationRequest call() throws Exception {

        // if this NetworkNode was visited, just forward the request to where it has not been yet (or parent)
        if (getVisitedNodes().contains(node)){
            if (NetworkNode.DEBUG_INFO) System.out.println(" node visited, forwarding...");

            manageRequestWhenThisNodeIsAlreadyChecked();
//            forwardedWithoutWaiting = true;

            if (NetworkNode.DEBUG_INFO) System.out.println(clientId + " request forwarded");

            return this;
        }

        // Node has not been visited
        if (NetworkNode.DEBUG_INFO) System.out.println("Node has not been visited");
        final Boolean[] resourcesLocallyAllocated = new Boolean[1];
        resourcesLocallyAllocated[0] = false;

        // Try to allocate resources locally

        if (NetworkNode.DEBUG_INFO) {
            System.out.println("To allocate (trying to allocate): ");
            resourcesToAllocate.forEach((k, v) -> System.out.println(k + ":" + v));
        }

        resourcesToAllocate.entrySet().removeIf(e -> {
            Integer allocatedAmount =
                    node.getResourceManager().tryToAllocateResource(clientId, e.getKey(), e.getValue());

            if (allocatedAmount != 0) {
                allocationHistory.removeIf(req -> req.getLocation().equals(this.node) && req.getAmount() == 0);
                allocationHistory.add(new RequestAllocationInfo(e.getKey(), allocatedAmount, this.node));
            }
            else if (allocationHistory.stream().noneMatch(
                            req -> req.getLocation().equals(this.node)))
                allocationHistory.add(new RequestAllocationInfo(e.getKey(), allocatedAmount, this.node));

            e.setValue(e.getValue() - allocatedAmount);

            resourcesLocallyAllocated[0] = resourcesLocallyAllocated[0] || allocatedAmount > 0;
            return e.getValue() == 0;
        });

        if (NetworkNode.DEBUG_INFO) {
            System.out.println("Allocated resources:\n" +
                                       node.getResourceManager()
                                               .getAllocatedResources()
                                               .entrySet()
                                               .stream()
                                               .filter(e -> Objects.equals(e.getKey().getKey(), clientId))
                                               .collect(Collectors.toList()));

//            System.out.println(clientId + " isCompleted: " + isCompleted());
        }

        if (isCompleted()) {
            if (NetworkNode.DEBUG_INFO) System.out.println(clientId + " request fully completed!");

            allocationStatus = AllocationStatus.ALLOCATED;
            node.setListenPort(null);
            if (NetworkNode.DEBUG_INFO) System.out.println("listenPort: null");
            if (NetworkNode.DEBUG_INFO) System.out.println("sending status to all waiting nodes...");
            sendStatusToAllWaitingNodes();

            return this;
        }

        if (resourcesLocallyAllocated[0]) {
            // if allocated something, but not completed => forward the request and wait for the response on listenPort
            if (NetworkNode.DEBUG_INFO) System.out.println("allocated resources, but not finished...");
            if (serverSocket == null) serverSocket = new ServerSocket(0);
            node.setListenPort(serverSocket.getLocalPort());

            if (NetworkNode.DEBUG_INFO) {
                System.out.println("visited nodes:");
                for (Destination d :
                        getVisitedNodes()) {
                    System.out.print(d.getStringForProtocol() + " ");
                }
                System.out.println();
//                System.out.println(this.getVisitedNodes().toString());
            }

            for (Destination child : node.getChildrenNodes()) {
//                if (NetworkNode.DEBUG_INFO){
//                    System.out.println("child: " + child);
//                    for (Destination d :
//                            getVisitedNodes()) {
//                        System.out.println(d + "=?" + child + ": " + child.equals(d));
//                    }
////                    System.out.println(child.equals(getVisitedNodes()));
//                }

                if (this.getVisitedNodes().contains(child)) continue;

                if (NetworkNode.DEBUG_INFO) System.out.println("Forwarding request to " + child.getStringForProtocol());

                child.forwardAllocationRequest(this);

                if (NetworkNode.DEBUG_INFO) System.out.println("Waiting for child response" + child.getStringForProtocol());
                // await child response
                AllocationRequest childResponse =
                        new AllocationRequest(TCPHandler.getMessage(serverSocket.accept()), node);
                if (NetworkNode.DEBUG_INFO) System.out.println("answer came, waiting stopped");

                if (childResponse.getAllocationStatus().equals(AllocationStatus.ALLOCATED)) {
                    allocationStatus = AllocationStatus.ALLOCATED;
                    resourcesToAllocate = childResponse.getResourcesToAllocate();
                    allocationHistory = childResponse.getAllocationHistory();
                    allocationHistory.stream().map(RequestAllocationInfo::getLocation)
                            .filter(destination -> destination.equals(node))
                            .forEach(destination -> destination.setListenPort(null));
                    node.setListenPort(null);
                    return this;
                }

                if (childResponse.getAllocationStatus().equals(AllocationStatus.ALLOCATE)) {
                    allocationStatus = childResponse.getAllocationStatus();
                    this.resourcesToAllocate = childResponse.getResourcesToAllocate();
                    this.allocationHistory = childResponse.getAllocationHistory();
                }
            }
            if (NetworkNode.DEBUG_INFO) System.out.println("Every child checked");

            if (node.getParentNode() == null && allocationStatus.equals(AllocationStatus.ALLOCATE)) {
                // when all children checked and this is MASTER PARENT Node
                if (NetworkNode.DEBUG_INFO) System.out.println(" and it is MASTER PARENT... :(");
                allocationStatus = AllocationStatus.FAILED;

                node.getResourceManager().deallocate(clientId);
                node.setListenPort(null);
                if (NetworkNode.DEBUG_INFO) System.out.println("listenPort: null");

                if (NetworkNode.DEBUG_INFO) System.out.println("sending FAILED status to every waiting Node");
                sendStatusToAllWaitingNodes();
                return this;
            }

            if (node.getParentNode() != null && allocationStatus.equals(AllocationStatus.ALLOCATE)) {
                // when all children checked and Parent exists
                try {
                    if (NetworkNode.DEBUG_INFO) System.out.println("trying to set Parent's listenPort");
                    node.getParentNode().setListenPort(
                            getVisitedNodes().stream()
                                    .filter(destination -> destination.equals(node.getParentNode()))
                                    .map(Destination::getListenPort).findAny().orElse(null));
                } catch (NullPointerException ignored) {}

                if (NetworkNode.DEBUG_INFO) System.out.println("parent: " + this.node.getParentNode());

                if (node.getParentNode().getListenPort() != null) {
                    TCPHandler.sendMessage(buildProtocol(false), new Socket(
                            node.getParentNode().getIp(), node.getParentNode().getListenPort()
                    ));
                } else node.getParentNode().forwardAllocationRequest(this);

                if (NetworkNode.DEBUG_INFO) System.out.println("Waiting for final response...");
                // Await the final response
                AllocationRequest finalResponse =
                        new AllocationRequest(TCPHandler.getMessage(serverSocket.accept()), node);

                if (finalResponse.getAllocationStatus().equals(AllocationStatus.FAILED)) {

                    node.getResourceManager().deallocate(clientId);
                    if (NetworkNode.DEBUG_INFO) System.out.println("allocation failed.");
                } else {
                    allocationStatus = finalResponse.getAllocationStatus();
                    resourcesToAllocate = finalResponse.getResourcesToAllocate();
                    allocationHistory = finalResponse.getAllocationHistory();
                    allocationHistory.stream().map(RequestAllocationInfo::getLocation)
                            .filter(destination -> destination.equals(node))
                            .forEach(destination -> destination.setListenPort(null));
                    if (NetworkNode.DEBUG_INFO) System.out.println("allocation successful");
                }
                node.setListenPort(null);
                if (NetworkNode.DEBUG_INFO) System.out.println("cleared listenPort");

                return this;
            }

        } else {
            // if nothing allocated and not completed => forward the request and move on
            manageRequestWhenThisNodeIsAlreadyChecked();

        }

        return this;
    }

    private void manageRequestWhenThisNodeIsAlreadyChecked() throws IOException {
        for (Destination child : node.getChildrenNodes()) {
            if (this.getVisitedNodes().contains(child)) continue;
            child.forwardAllocationRequest(this);
            forwardedWithoutWaiting = true;
            return;
        }

        if (node.getParentNode() == null) {
            // allocation FAILED: deallocate ALL (also outer) resources, send info to ComNode
            if (NetworkNode.DEBUG_INFO) System.out.println("allocation FAILED: deallocate ALL (also outer) resources, send info to ComNode");
            allocationStatus = AllocationStatus.FAILED;

            // in case there are allocated resources
            node.getResourceManager().deallocate(clientId);
            node.setListenPort(null);
            if (NetworkNode.DEBUG_INFO) System.out.println("listenPort: null");

            sendStatusToAllWaitingNodes();

        } else {


            try {
                if (node.getParentNode() != null) {
                    if (NetworkNode.DEBUG_INFO) System.out.println("parent: " + node.getParentNode().getStringForProtocol());
                    node.getParentNode().setListenPort(
                            allocationHistory.stream().map(RequestAllocationInfo::getLocation)
                                    .filter(destination -> destination.equals(node.getParentNode()))
                                    .map(Destination::getListenPort)
                                    .findAny().orElse(null));
                }
            } catch (NullPointerException ignored) {}

            if (NetworkNode.DEBUG_INFO) System.out.println("updated parent: " + node.getParentNode().getStringForProtocol());

//                    anyMatch(
//                    req -> req.getLocation().getIp().getHostAddress().equals(node.getParentNode().getIp().getHostAddress()) &&
//                    req.getLocation().getPort().equals(node.getParentNode().getPort())) && ) {
//                Socket socket = new Socket(node.getParentNode().getIp(), node.getListenPort());
//                TCPHandler.sendMessage(this.buildProtocol(false), socket);
//                return;
//            }
            if (node.getParentNode().getListenPort() != null) {
                TCPHandler.sendMessage(this.buildProtocol(false),
                                       new Socket(node.getParentNode().getIp(), node.getParentNode().getListenPort()));
            } else node.getParentNode().forwardAllocationRequest(this);
            forwardedWithoutWaiting = true;
        }
    }

    private void sendStatusToAllWaitingNodes() {
        Socket socket;

        if (!comNode.equals(node)) {
            try {
//                if (NetworkNode.DEBUG_INFO) System.out.println("TO COMNODE");
                socket = new Socket(comNode.getIp(), comNode.getListenPort());
                if (NetworkNode.DEBUG_INFO) System.out.println("INFO TO COMNODE");
                TCPHandler.sendMessage(buildProtocol(false), socket);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (RequestAllocationInfo info : allocationHistory) {
            if (info.getLocation().getListenPort() == null) continue;
            if (info.getLocation().equals(comNode) || info.getLocation().equals(node)) continue;
            if (!info.getLocation().equals(node.getParentNode()) && info.getAmount() == 0) continue;

            try {
//                if (NetworkNode.DEBUG_INFO) System.out.println("TO REST");
                socket = new Socket(info.getLocation().getIp(), info.getLocation().getListenPort());
                if (NetworkNode.DEBUG_INFO) System.out.println("INFO TO NODE FROM HISTORY");
                TCPHandler.sendMessage(buildProtocol(false), socket);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
