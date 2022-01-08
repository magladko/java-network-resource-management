import javafx.util.Pair;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ResourceManager {

    private ConcurrentHashMap<Character, Integer> availableResources;
    private ConcurrentHashMap<Pair<Integer,Character >, Integer> allocatedResources;
    private ExecutorService allocationRequestsExecutor;
//    private LinkedBlockingQueue<Future<String>> allocationRequests;

    public ResourceManager(List<String> availableResourcesStringList) {
        this.availableResources = new ConcurrentHashMap<>();
        this.allocatedResources = new ConcurrentHashMap<>();

        availableResourcesStringList.forEach(str ->
            availableResources.put(str.charAt(0), Integer.parseInt(str.split(":")[1]))
        );

//        Arrays.stream(availableResourcesString.split(" "))
//                .forEach(str -> availableResources.put(str.charAt(0), Integer.parseInt(str.split(":")[1])));
//        this.allocationRequests = new LinkedBlockingQueue<>();
        this.allocationRequestsExecutor = Executors.newSingleThreadExecutor();
    }

    public Future<AllocationRequest> requestAllocation(AllocationRequest request) {
        return allocationRequestsExecutor.submit(request);
    }

    public ExecutorService getAllocationRequestsExecutor() {
        return allocationRequestsExecutor;
    }

    public Integer getAvailableResourceAmount (Character resourceType) {
        return availableResources.get(resourceType);
    }

    public ConcurrentHashMap<Pair<Integer, Character>, Integer> getAllocatedResources() {
        return allocatedResources;
    }

    public Integer tryToAllocateResource(Integer clientId, Character type, Integer requestedAmount) {
        if (availableResources.get(type) == null) return 0;
        Integer allocatedAmount = Math.min(availableResources.get(type), requestedAmount);
//                Math.max(availableResources.get(type) - requestedAmount, 0);
//        Integer allocatedAmount = Math.max(requestedAmount - availableResources.get(type), requestedAmount);
        availableResources.replace(
                type,
                availableResources.get(type) - allocatedAmount
        );
        allocatedResources.put(new Pair<>(clientId, type), allocatedAmount);
        return allocatedAmount;
    }

    public void deallocate(Integer clientId) {
        allocatedResources.entrySet().removeIf(e -> {
            if (Objects.equals(e.getKey().getKey(), clientId)) {
                availableResources.replace(
                        e.getKey().getValue(),
                        availableResources.get(e.getKey().getValue()) + e.getValue()
                );
                e.setValue(0);
            }
            return e.getValue() == 0;
        });
    }
}
