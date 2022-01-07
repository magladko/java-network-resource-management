import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Class managing connections between Client to ComNode.
 * Also manages HELLO communicate between new children and parent Nodes.
 * All communication between Nodes is made inside @AllocationRequest class.
 * Also contains static TCP connection handling methods.
 */
public class TCPHandler implements Runnable {

    Socket socket;
    NetworkNode node;

    public TCPHandler(Socket socket, NetworkNode node) {
        this.socket = socket;
        this.node = node;
    }

    @Override
    public void run() {

        System.out.println("RUN!");

        BufferedReader inFromClient;
        PrintWriter outToClient;


//        List<String[]> in = getMessage(socket);
        try  {

            System.out.println("ADDRESS " + socket.getLocalAddress());
            if (node.getIp() == null) {
//                node.setPort(socket.getLocalPort());
                node.setIp(socket.getLocalAddress());
            }

            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToClient = new PrintWriter(socket.getOutputStream(), true);

            List<String[]> in = new ArrayList<>();
            in.add(inFromClient.readLine().split(" "));
            AllocationRequest request;
            List<String[]> out;

            in.forEach(tab -> System.out.println(Arrays.asList(tab)));

            //            case "ALLOCATE":
            //                /**
            //                 * ALLOCATE <ComNodeIP>:<ComNodePORT>:<listenPort>
            //                 * <clientId> <zasób>:<liczność> [<zasób>:liczność]
            //                 * [<zasób>:<liczność>:<ip węzła>:<port węzła>:<listenPort>]
            //                 * [<zasób>:<liczność>:<ip węzła>:<port węzła>:<listenPort>]
            //                 * [...]
            //                 */
            //                try {
            //                    request = node.getResourceManager()
            //                            .requestAllocation(new AllocationRequest(in, node))
            //                            .get();
            //                } catch (InterruptedException | ExecutionException e) {
            //                    e.printStackTrace();
            //                    request = new AllocationRequest(in, node);
            //                    System.exit(1);
            //                }
            //
            //                break;
            //            case "ALLOCATED":
            //                /**
            //                 * ALLOCATED <ComNodeIP>:<ComNodePORT>:<listenPort>
            //                 * <clientId>
            //                 * <zasób>:<liczność>:<ip węzła>:<port węzła>:<listenPort>
            //                 * [<zasób>:<liczność>:<ip węzła>:<port węzła>:<listenPort>]
            //                 * [...]
            //                 */
            //                break;
            //            case "FAILED":
            //                /**
            //                 * FAILED <ComNodeIP>:<ComNodePORT>:<listenPort>
            //                 * <clientId>
            //                 * [<zasób>:<liczność>:<ip węzła>:<port węzła>:<listenPort>]
            //                 * [<zasób>:<liczność>:<ip węzła>:<port węzła>:<listenPort>]
            //                 * [...]
            //                 */
            //                break;

            if ("TERMINATE".equals(in.get(0)[0])) {

                System.out.println("TERMINATION BEGAN");

                // send termination info everywhere possible and shutdown
//                if (node.getResourceManager().getAllocationRequestsExecutor() != null)
                node.getResourceManager().getAllocationRequestsExecutor().shutdownNow();

                System.out.println("EXECUTOR did SHUTDOWN");

                inFromClient.close();
                outToClient.close();
                socket.close();

                node.getEveryNeighbour().stream().filter(Objects::nonNull)
                        .forEach(destination -> {
                            try {
                                Socket terminationSocket = new Socket(destination.getIp(), destination.getPort());
//                                outToClient.println("TERMINATE");
                                sendMessage(Collections.singletonList(new String[]{"TERMINATE"}), terminationSocket);
                                terminationSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

//                node.getThreadPool().shutdownNow();

                System.exit(0);

            } else if ("HELLO".equals(in.get(0)[0])) {
                /**
                 * adds children to childrenNodeList for this Node
                 * protocol schema:
                 * HELLO childNodeId:childNodeIp:childNodePort
                 */

                try {
                    node.getChildrenNodes().add(new Destination(
                            Integer.parseInt(in.get(0)[1].split(":")[0]),
                            InetAddress.getByName(in.get(0)[1].split(":")[1]),
                            Integer.parseInt(in.get(0)[1].split(":")[2])));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            } else {
                // connection from client => <identyfikator> <zasób>:<liczność> [<zasób>:liczność]

                Integer clientId = Integer.parseInt(in.get(0)[0]);
//                node.setResourceManager(new ResourceManager(
//                        Arrays.stream(in.get(0)).skip(1).collect(Collectors.toList())
//                ));
    //                Set<Resource> requestedResources = Arrays.stream(in.get(0))
    //                        .skip(1).map(NetworkNode::mapStrToResource).collect(Collectors.toSet());

                System.out.println("allocation request received...");
                System.out.println("allocating: " + String.join("\n\t",
                                                                new AllocationRequest(clientId,
                                                                                      in,
                                                                                      node
                                                                ).getProtocolContent()));

                try {
                    request = node.getResourceManager()
                            .requestAllocation(new AllocationRequest(clientId, in, node))
                            .get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    request = new AllocationRequest(in, node);
                    System.exit(1);
                }

                System.out.println("Allocation process done.");

                if (request.isCompleted()) {
                    System.out.println("Allocation completed!");
                    // formatting the response to match client specification
                    List<String> response = new ArrayList<>(); //request.getProtocolContentTab(" ")
    //                response.set(0, new String[]{"ALLOCATED"});
                    response.add("ALLOCATED");
                    for (int i = 2; i < request.getProtocolContent().size(); i++) {
                        response.add(Arrays.stream(request.getProtocolContent().get(i).split(":")).limit(4).collect(
                                Collectors.joining(":")));
                    }

//                    response.set(0, new String[]{"ALLOCATED"});


//                    response.stream().map(tab -> new String[]{tab[0].split(":")[0]});

                    System.out.println("AA");
//                    response.forEach(tab -> Arrays.stream(tab).forEach(System.out::print));



//                    response = response.stream().skip(1)
//                            .filter(tab -> Integer.parseInt(tab[1]) == 0)
//                            .map(tab -> {
//                                for (String a :
//                                        tab) {
//                                    System.out.print(a + " ");
//                                }
//                                System.out.println();
//                                return new String[]{Arrays.stream(tab).limit(4).collect(Collectors.joining(":"))};
//                            })
//                            .collect(Collectors.toList());

                    System.out.println("AAA");

//                    response.add(0, new String[]{"ALLOCATED"});

                    /**
                     * message schema:
                     *
                     * ALLOCATED
                     * <zasób>:<liczność>:<ip węzła>:<port węzła>
                     *     [...]
                     *
                     */

//                    System.out.println(String.join("\n", request.getProtocolContent()));
                    System.out.println(String.join("\n", response));
                    outToClient.println(String.join("\n", response));

//                    sendMessage(request.getProtocolContentTab(" "), socket);
                } else {
                    System.out.println("Allocation failed...");
                    outToClient.println("FAILED");
//                    sendMessage(Collections.singletonList(new String[]{"FAILED"}), socket);
                }

            }

            inFromClient.close();
            outToClient.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    private void localMessageStatusHandler(List<String[]> producedProtocolMessage) {
//        switch (producedProtocolMessage.get(0)[0]) {
//            case "ALLOCATE":
//                while ()
//                break;
//            case "ALLOCATED":
//
//        }
//    }

    public static List<String[]> getMessage(Socket socket) {
        ArrayList<String[]> res = new ArrayList<>();
        try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;

            while ((line = inFromClient.readLine()) != null) {
                System.out.println(line);
                res.add(line.split(" "));
                return res;
//                if (line.equals("TERMINATE") || line.)
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//        System.out.println(res);
        return res;
    }

//    public static List<String[]> getSingleMessageAndMaintainConnection(Socket socket) {
//        ArrayList<String[]> res = new ArrayList<>();
//        try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//            String line;
//
//            while ((line = inFromClient.readLine()) != null) {
//                System.out.println(line);
//                res.add(line.split(" "));
//                return res;
////                if (line.equals("TERMINATE") || line.)
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
////        System.out.println(res);
//        return res;
//    }

    public static Boolean sendMessage(List<String[]> msg, Socket socket) {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.print(String.join("\n", convertInputListToStrList(msg)));

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static List<String> convertInputListToStrList(List<String[]> in) {
        return in.stream().map(strTab -> String.join(" ", strTab)).collect(Collectors.toList());
    }

}
