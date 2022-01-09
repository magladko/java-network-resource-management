import javafx.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ResourceManager {

    private final ConcurrentHashMap<Character, Integer> totalAvailableResources;
    private ConcurrentHashMap<Pair<Integer,Character >, Integer> allocatedResources;
    private ConcurrentHashMap<Pair<Integer,Character >, Integer> pendingResources;
    private ExecutorService allocationRequestsExecutor;

    public ResourceManager(List<String> availableResourcesStringList) {
        this.totalAvailableResources = new ConcurrentHashMap<>();
        this.allocatedResources = new ConcurrentHashMap<>();
        this.pendingResources = new ConcurrentHashMap<>();

        availableResourcesStringList.forEach(str ->
            totalAvailableResources.put(str.charAt(0), Integer.parseInt(str.split(":")[1]))
        );
        this.allocationRequestsExecutor = Executors.newSingleThreadExecutor();
    }

    public Future<AllocationRequest> requestAllocation(AllocationRequest request) {
        return allocationRequestsExecutor.submit(request);
    }

    public ExecutorService getAllocationRequestsExecutor() {
        return allocationRequestsExecutor;
    }

    public Integer getAvailableResourceAmount (Character resourceType) {
        return totalAvailableResources.get(resourceType)
                - allocatedResources.entrySet().stream()
                .filter(e -> e.getKey().getValue() == resourceType)
                .mapToInt(Map.Entry::getValue).sum()
                - pendingResources.entrySet().stream()
                .filter(e -> e.getKey().getValue() == resourceType)
                .mapToInt(Map.Entry::getValue).sum();
    }

    public Map<Character, Integer> getAvailableResources() {
        Map<Character, Integer> availableResources = new HashMap<>();
        getTotalAvailableResources().forEach((type, amount) -> {
            Integer availableAmount = getAvailableResourceAmount(type);
            if (availableAmount != 0) availableResources.put(type, availableAmount);
        });
        return availableResources;
    }

    public ConcurrentHashMap<Character, Integer> getTotalAvailableResources() {
        return totalAvailableResources;
    }

    public void printAvailableResources() {
        System.out.print("Available resources: ");

        getAvailableResources().forEach((type, amount) -> System.out.print(type + ":" + amount + " "));
        System.out.println();
    }

    public void printAllocatedResources() {
        System.out.print("Allocated resources: ");
        allocatedResources.forEach(
                (pair, amount) -> System.out.print(pair.getKey() + ": " + pair.getValue() + ":" + amount + " ; "));
        System.out.println();
    }

    public void printNotAllocatedResources() {
        totalAvailableResources.forEach((type, amount) -> {
            System.out.print(type + ":");
            System.out.print(amount - allocatedResources.entrySet()
                    .stream().filter(e -> e.getKey().getValue().equals(type)).mapToInt(Map.Entry::getValue).sum() + " ");
        });
        System.out.println();
    }

    public ConcurrentHashMap<Pair<Integer, Character>, Integer> getAllocatedResources() {
        return allocatedResources;
    }

    public ConcurrentHashMap<Pair<Integer, Character>, Integer> getPendingResources() {
        return pendingResources;
    }

    public Integer tryToAllocateResource(Integer clientId, Character type, Integer requestedAmount) {
        if (getAvailableResources().get(type) == null || totalAvailableResources.get(type) == 0) return 0;
        Integer allocatedAmount = Math.min(
                getAvailableResources().get(type),
                requestedAmount
        );

        pendingResources.put(new Pair<>(clientId, type), allocatedAmount);

        if (NetworkNode.DEBUG_INFO) System.out.println("Pending amount: " + clientId + ":" + type + ":" + allocatedAmount);
        return allocatedAmount;
    }

    public void deallocate(Integer clientId) {
        if (NetworkNode.DEBUG_INFO) System.out.println("DEALLOCATING for clientId:" + clientId);

        pendingResources.entrySet().removeIf(e -> e.getKey().getKey().equals(clientId));
    }

    public void allocate(Integer clientId) {

        pendingResources.entrySet().removeIf(e -> {
            if (allocatedResources.putIfAbsent(e.getKey(), e.getValue()) != null)
                allocatedResources.replace(e.getKey(), allocatedResources.get(e.getKey()) + e.getValue());
            return Objects.equals(e.getKey().getKey(), clientId);
        });

        if (NetworkNode.REPORT_RESOURCES_LEFT) {
            System.out.print("Resources left: ");
            printNotAllocatedResources();
        }
    }
}
