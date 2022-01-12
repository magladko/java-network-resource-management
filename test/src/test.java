import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;


public class test {

    public static void main(String[] args) {
//        Resource r = new Resource('A', 10);
//        HashMap<Character, Integer> m = new HashMap<>();
//
//        m.put('A', 4);
//        m.put('B', 4);
//        m.put('C', 4);
//
//        System.out.println(m.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(" ", "aa", "bb")));

//        try {
//            ServerSocket serverSocket = new ServerSocket(0);
//
//            System.out.println(serverSocket.getLocalPort());
//            System.out.println(serverSocket.getInetAddress().getHostAddress());
//            System.out.println(serverSocket.getInetAddress().getAddress());
//            System.out.println(serverSocket.getInetAddress().getHostName());
//            System.out.println(serverSocket.getInetAddress().getCanonicalHostName());
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        System.out.println(new Pair<Integer, Character>(3, 'a').equals(new Pair<Integer, Character>(1, 'a')));


//        String[] str = new String[]{"aa", "BB"};
//        ArrayList<String[]> arrayList = new ArrayList<>();
//        arrayList.add(str);
//        arrayList.add(str);
//        arrayList.add(str);
//
////        System.out.println(arrayList.stream().skip(1).collect(Collectors.toList()).toString());
//
//        try {
//            ServerSocket serverSocket = new ServerSocket(0);
//            InetAddress address = InetAddress.getByName(serverSocket.getInetAddress().toString());
//            System.out.println(address);
//            System.out.println(new Socket().getInetAddress());
//            System.out.println(new Socket().getLocalAddress());
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


//        System.out.println(convertInputListToStrList(arrayList));
    }

    private static List<String> convertInputListToStrList(List<String[]> in) {
        return in.stream().map(strTab -> String.join(" ", strTab)).collect(Collectors.toList());
    }

}
