public class RequestAllocationInfo {

    private Character type;
    private Integer amount;
    private Destination location;

    public RequestAllocationInfo(Character type, Integer amount, Destination location) {
        this.type = type;
        this.amount = amount;
        this.location = location;
    }

    public Integer getAmount() {
        return amount;
    }

    public Destination getLocation() {
        return location;
    }

    @Override
    public String toString() {
        String addListenPort = "";
        if (location.getListenPort() != null) addListenPort = ":" + location.getListenPort();
        return type.toString() + ":" + amount + ":" + location.getIp().getHostAddress()
                + ":" + location.getPort() + addListenPort;
    }

    public String toStringNoListenPort() {
        return type.toString() + ":" + amount + ":" + location.getIp().getHostAddress()
                + ":" + location.getPort();
    }
}
