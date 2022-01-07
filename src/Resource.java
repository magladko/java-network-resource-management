import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Resource {



//    private final Character type;
//    private final Integer totalAmount;
//    private volatile Boolean full;
//    private ConcurrentHashMap<Integer, Integer> allocatedClientAmount;
//    private ConcurrentHashMap<Integer, Boolean> allocationStatus;
////    private ConcurrentHashMap<Integer, Integer> reservedClientAmount;
////    private LinkedBlockingQueue<List<Object>> waitingClientResource;
//    private LinkedBlockingQueue<Future<>>

//    public Resource(Character type, Integer totalAmount) {
//        this.type = type;
//        this.totalAmount = totalAmount;
//        full = (totalAmount > 0);
//        this.allocatedClientAmount = new ConcurrentHashMap<>();
//        this.waitingClientResource = new LinkedBlockingQueue<>();
//
////        allocatedClientAmount.put(55, 3);
////        allocatedClientAmount.put(45, 1);
////        allocatedClientAmount.put(32, 4);
//
////        try {
////            while (true) {
////                List<Object> request = waitingClientResource.take();
////                if (getAvailableResourceAmount() >= request.get) {
////
////                }
////            }
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//
//    }
//
//    public Boolean tryToAllocateResource(Integer clientId, Integer requestedAmount, NetworkNode node) {
//        waitingClientResource.offer(Arrays.asList(clientId, requestedAmount, node));
//
//    }
//
////    public LinkedBlockingQueue<Pair<Integer, Integer>> getWaitingClientResource() {
////        return waitingClientResource;
////    }
//
//    public Character getType() {
//        return type;
//    }
//
//    public Integer getTotalAmount() {
//        return totalAmount;
//    }
//
//    public Boolean isFull() {
//        return full;
//    }
//
//    public Integer getAvailableResourceAmount() {
//        return totalAmount - allocatedClientAmount.values().stream().reduce(0, Integer::sum);
//    }

//    @Override
//    public String toString() {
//        return type.toString() + ":" + getAvailableResourceAmount();
//    }
}
