public class RequestAllocationInfo {

    private Character type;
    private Integer amount;
    private Destination location;

    public RequestAllocationInfo(Character type, Integer amount, Destination location) {
        this.type = type;
        this.amount = amount;
        this.location = location;
    }

    public Character getType() {
        return type;
    }

    public void setType(Character type) {
        this.type = type;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Destination getLocation() {
        return location;
    }

    public void setLocation(Destination location) {
        this.location = location;
    }

    @Override
    public String toString() {
        String addListenPort = "";
        if (location.getListenPort() != null) addListenPort = ":" + location.getListenPort();
        return type.toString() + ":" + amount + ":" + location.getIp().getHostAddress()
                + ":" + location.getPort() + addListenPort;
    }
}
