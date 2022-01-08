import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class AllocationRequest implements Callable<AllocationRequest> {

    private final Integer clientId;
    private AllocationStatus allocationStatus;
    private Map<Character, Integer> resourcesToAllocate;

    private Destination comNode;
    private List<RequestAllocationInfo> allocationHistory;

    private final NetworkNode node;

    private ServerSocket serverSocket;

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

        serverSocket = new ServerSocket(0);
        node.setListenPort(serverSocket.getLocalPort());
        this.comNode = node;

        this.clientId = clientId;
        this.node = node;
        this.allocationHistory = new ArrayList<>();

        resourcesToAllocate = Arrays.stream(in.get(0)).skip(1).collect(Collectors.toMap(
                str -> str.split(":")[0].charAt(0),
                str -> Integer.parseInt(str.split(":")[1])
        ));
    }

    /**
     * Constructor used when request forwarded from NetworkNode.
     */
    public AllocationRequest(List<String[]> protocolMessage, NetworkNode node) throws IOException {

        this.node = node;

        // LINE 1 => ALLOCATE <ComNodeIP>:<ComNodePORT>:<listenPort>    // OR
        // LINE 1 => ALLOCATED                                          // OR
        // LINE 1 => FAILED
        allocationStatus = AllocationStatus.valueOf(protocolMessage.get(0)[0]);

        if (allocationStatus.equals(AllocationStatus.ALLOCATE)) {
            // LINE 1 => ALLOCATE <ComNodeIP>:<ComNodePORT>:<listenPort>
            comNode = new Destination(
                    InetAddress.getByName(protocolMessage.get(0)[1]),
                    Integer.parseInt(protocolMessage.get(0)[2]),
                    Integer.parseInt(protocolMessage.get(0)[3])
            );

            // LINE 2 => <clientId> <zasób>:<liczność> [<zasób>:liczność]
            clientId = Integer.parseInt(protocolMessage.get(1)[0]);
            resourcesToAllocate = Arrays.stream(protocolMessage.get(1)).skip(1).collect(Collectors.toMap(
                    str -> str.split(":")[0].charAt(0),
                    str -> Integer.parseInt(str.split(":")[1])
            ));

            // LINE 3... => <zasób>:<liczność>:<ip węzła>:<port węzła>[:<listenPort>]
            for (int i = 2; i < protocolMessage.size(); i++) {
                String[] infoLine = protocolMessage.get(i)[0].split(":");
                allocationHistory.add(new RequestAllocationInfo(
                        infoLine[0].charAt(0),
                        Integer.parseInt(infoLine[1]),
                        new Destination(
                                InetAddress.getByName(infoLine[2]),
                                Integer.parseInt(infoLine[3]),
                                infoLine.length == 5 ? Integer.parseInt(infoLine[4]) : null
                        )
                ));
            }
        } else {
            this.clientId = Integer.parseInt(protocolMessage.get(1)[0]);
        }
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

    public AllocationStatus getAllocationStatus() {
        return allocationStatus;
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

    /**
     * Allocation Request protocol types:
     *
     * @param toClient = false
     * ALLOCATE <ComNodeIP>:<ComNodePORT>:<listenPort>
     * <clientId> <zasób>:<liczność> [<zasób>:liczność]
     * [<zasób>:<liczność>:<ip węzła>:<port węzła>[:<listenPort>]]
     * [<zasób>:<liczność>:<ip węzła>:<port węzła>[:<listenPort>]]
     * [...]
     *
     * ALLOCATED
     * <clientId>
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
                if (comNode != null) result += " " + comNode.toString();
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
                break;
            case ALLOCATED:
                result += "\n";
                if (!toClient) result += clientId + "\n";
                else {
                    resultBuilder1 = new StringBuilder(result);
                    for (RequestAllocationInfo info : allocationHistory) {
                        resultBuilder1.append(info.toString()).append("\n");
                    }
                    result = String.valueOf(resultBuilder1);
                }
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

        // TODO CHECK IF EQUALS WORKS
        // if this NetworkNode was visited, just forward the request to where it has not been yet (or parent)
        if (getVisitedNodes().contains(node)){
            if (NetworkNode.DEBUG_INFO) System.out.println(clientId + " node visited, forwarding...");

            manageRequestWhenThisNodeIsAlreadyChecked();

            if (NetworkNode.DEBUG_INFO) System.out.println(clientId + " request forwarded");

            return this;
        }

        // Node has not been visited

        final Boolean[] resourcesLocallyAllocated = new Boolean[1];

        // Try to allocate resources locally

        if (NetworkNode.DEBUG_INFO) resourcesToAllocate.forEach((k, v) -> System.out.println(k + ":" + v));

        resourcesToAllocate.entrySet().removeIf(e -> {
            Integer allocatedAmount =
                    node.getResourceManager().tryToAllocateResource(clientId, e.getKey(), e.getValue());

            allocationHistory.add(new RequestAllocationInfo(e.getKey(), e.getValue(), this.node));

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

            System.out.println(clientId + " isCompleted: " + isCompleted());
        }

        if (isCompleted()) {
            if (NetworkNode.DEBUG_INFO) System.out.println(clientId + " request fully completed!");

            allocationStatus = AllocationStatus.ALLOCATED;

            sendStatusToAllWaitingNodes();

            return this;
        }

        if (resourcesLocallyAllocated[0]) {
            // if allocated something, but not completed => forward the request and wait for the response on listenPort

            node.setListenPort(serverSocket.getLocalPort());

            for (Destination child : node.getChildrenNodes()) {
                if (this.getVisitedNodes().contains(child)) continue;

                if (NetworkNode.DEBUG_INFO) System.out.println("Forwarding request...");

                child.forwardAllocationRequest(this);

                // await child response
                AllocationRequest childResponse =
                        new AllocationRequest(TCPHandler.getMessage(serverSocket.accept()), node);

                if (childResponse.getAllocationStatus().equals(AllocationStatus.ALLOCATED)) {
                    allocationStatus = AllocationStatus.ALLOCATED;
                    return this;
                }

                if (childResponse.getAllocationStatus().equals(AllocationStatus.ALLOCATE)) {
                    this.resourcesToAllocate = childResponse.getResourcesToAllocate();
                    this.allocationHistory = childResponse.getAllocationHistory();
                }
            }

            if (node.getParentNode() == null && allocationStatus.equals(AllocationStatus.ALLOCATE)) {
                // when all children checked and this is MASTER PARENT Node
                allocationStatus = AllocationStatus.FAILED;

                node.getResourceManager().deallocate(clientId);

                sendStatusToAllWaitingNodes();
                return this;
            }

            if (node.getParentNode() != null && allocationStatus.equals(AllocationStatus.ALLOCATE)) {
                // when all children checked and Parent exists
                node.getParentNode().forwardAllocationRequest(this);

                // Await the final response
                AllocationRequest finalResponse =
                        new AllocationRequest(TCPHandler.getMessage(serverSocket.accept()), node);

                if (finalResponse.getAllocationStatus().equals(AllocationStatus.FAILED)) {
                    node.getResourceManager().deallocate(clientId);
                }

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
            return;
        }

        if (node.getParentNode() == null) {
            // allocation FAILED: deallocate ALL (also outer) resources, send info to ComNode
            allocationStatus = AllocationStatus.FAILED;

            // in case there are allocated resources
            node.getResourceManager().deallocate(clientId);

            sendStatusToAllWaitingNodes();

        } else node.getParentNode().forwardAllocationRequest(this);
    }

    private void sendStatusToAllWaitingNodes() {
        Socket socket;

        if (!comNode.equals(node)) {
            try {
                socket = new Socket(comNode.getIp(), comNode.getListenPort());
                TCPHandler.sendMessage(buildProtocol(false), socket);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (RequestAllocationInfo info : allocationHistory) {
            if (!info.getLocation().equals(node.getParentNode()) &&
                    (info.getAmount() == 0 || info.getLocation().equals(node) || info.getLocation().equals(comNode))) {
                continue;
            }
            try {
                socket = new Socket(info.getLocation().getIp(), info.getLocation().getListenPort());
                TCPHandler.sendMessage(buildProtocol(false), socket);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
