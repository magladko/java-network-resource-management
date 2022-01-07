import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;

public class Destination {

    private Integer id;
    private InetAddress ip;
    private Integer port;
    private Integer listenPort;

    public Destination(Integer id, InetAddress ip, Integer port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public Destination(InetAddress ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    public Destination(String gateway) throws UnknownHostException {
        this.ip = InetAddress.getByName(gateway.split(":")[0]);
        this.port = Integer.parseInt(gateway.split(":")[1]);
    }

    public Destination(Integer id) {
        this.id = id;
    }

//    public Socket connect() throws IOException {
//        return new Socket(ip, port);
//    }


    public void forwardAllocationRequest(AllocationRequest request) throws IOException {
        Socket socket = new Socket(ip, port);
        TCPHandler.sendMessage(request.getProtocolContentTab(" "), socket);
    }

    public Integer getId() {
        return id;
    }

    public InetAddress getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getListenPort() {
        return listenPort;
    }

    public void setListenPort(Integer listenPort) {
        this.listenPort = listenPort;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        Destination d = (Destination) obj;
        return Objects.equals(d.getPort(), this.getPort()) && d.getIp() == this.getIp()
                || Objects.equals(d.getId(), this.getId());
    }
}
