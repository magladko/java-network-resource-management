import java.util.concurrent.ConcurrentHashMap;

public class Resource {

    private final Character type;
    private Integer amount;
    private final Integer nodeId;
    private ConcurrentHashMap<Integer, Integer> allocatedAmountPerClient;
    private volatile Integer clientId;

    public Resource(Character type, int amount, Integer nodeId) {
        this.type = type;
        this.amount = amount;
        this.nodeId = nodeId;
        this.clientId = null;
    }

    public Character getType() {
        return type;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }
}
