/**
 * Created by Serik Zhilibayev on 20.03.17.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Test{
            static final List<FMTFile> onlines = new ArrayList<FMTFile>();

            public static void main(String args[]) throws IOException {
                ServerSocket server = new ServerSocket(10777);
                while (true) {
                    Socket client = server.accept();
                    Runnable r = new MyThread(client);
                    new Thread(r).start();
                }
            }
}

class FMTFile implements Comparable<FMTFile>{
    private String ip;
    private String port;
    List<Fm> files;

    FMTFile(String ip, String port){
        this.ip = ip;
        this.port = port;
        files = new ArrayList<>();
    }

    void putList(List<String> list){
        files = new ArrayList<>();
        for (String aList : list) {
            files.add(new Fm(aList));
        }
    }

    public String toString() {
        return files.toString();
    }

    @Override
    public int compareTo(FMTFile o) {
        String comp1 = this.ip + this.port;
        String comp2 = o.ip + o.port;
        return comp1.compareTo(comp2);
    }
    int compareTo(String o) {
        String comp1 = this.ip + this.port;
        return comp1.compareTo(o);
    }
}
class Fm{
    String name;
    private String type;
    private String size;
    private String lastMod;
    private String ip;
    private String port;

     Fm(String str){
        String patterns[] = str.split(", ");
        name = patterns[0];
        type = patterns[1];
        size = patterns[2];
        lastMod = patterns[3];
        ip = patterns[4];
        port = patterns[5];
    }

    public String toString(){
        return "<" + name + ", " + type + ", " + size + ", " + lastMod + ", " + ip + ", " + port + ">";
    }
}

class MyThread implements Runnable {

    private Socket client;
    private String ip;
    private String port;

    MyThread(Socket client) {
        this.client = client;
        this.port = "" + client.getPort();
        this.ip = client.getInetAddress().getHostAddress();
    }

    public void run() {
        InputStream inp;
        BufferedReader brinp;
        PrintStream out;

        try {
            inp = client.getInputStream();
            brinp = new BufferedReader(new InputStreamReader(inp));
            out = new PrintStream(client.getOutputStream());
        } catch (IOException e) {
            return;
        }

        String line;
        Boolean handshake = false;
        Boolean isInf = false;

        while (true) {
            try {

                line = brinp.readLine();

                System.out.println("----->"+line);
                if (!handshake && line.equals("HELLO")){
                        out.println("HI");
                        System.out.println("In handshake");
                        handshake = true;

                        synchronized (Test.onlines) {
                            Test.onlines.add(new FMTFile(this.ip, this.port));
                        }

                }else if (handshake && !isInf){

                        List<String> list = new ArrayList<>();
                        Matcher m = Pattern.compile("\\<(.*?)\\>").matcher(line);
                        while(m.find()) {
                            list.add(m.group(1));
                        }
                        synchronized (Test.onlines){
                            List<FMTFile> x = Test.onlines;
                            int index = 0;
                            for (int i = 0; i < x.size(); i++){
                                if (x.get(i).compareTo(this.ip+this.port) == 0){
                                    index = i;
                                    break;
                                }
                            }

                            Test.onlines.get(index).putList(list);
                        }
                        isInf = true;
                        System.out.println(Test.onlines);

                }else if (handshake && isInf && line.length() >= 9 && line.substring(0,7).equals("SEARCH:")){
                        System.out.println(line.substring(7));
                        List<String> result = findUsers(line.substring(8));
                        if (result.size() > 0) {
                            System.out.println("FOUND: " + result);
                            out.println("FOUND: " + result.toString());
                        }else{
                            System.out.println("NOT FOUND");
                            out.println("NOT FOUND");
                        }
                }else if (handshake && isInf && line.equals("BYE")){

                        synchronized (Test.onlines){
                            List<FMTFile> x = Test.onlines;
                            int index = 0;
                            for (int i = 0; i < x.size(); i++){
                                if (x.get(i).compareTo(this.ip+this.port) == 0){
                                    index = i;
                                    break;
                                }
                            }
                            Test.onlines.remove(index);//Have to work with it!!!!!!
                        }

                        brinp.close();
                        out.close();
                        client.close();
                        return;

                }
            } catch (Exception e) {

                System.out.println(this.ip + " " + this.port + " - Client has gone");
                System.out.flush();
                synchronized (Test.onlines){
                    List<FMTFile> x = Test.onlines;
                    int index = 0;
                    for (int i = 0; i < x.size(); i++){
                        if (x.get(i).compareTo(this.ip+this.port) == 0){
                            index = i;
                            break;
                        }
                    }
                    Test.onlines.remove(index);//Have to work with it!!!!!!
                }

                //brinp.close();
                out.close();
                //client.close();

                System.out.println("");

                return;
            }
        }
    }

    private synchronized List<String> findUsers(String substring) {
        List<String> list = new ArrayList<>();
        int len = Test.onlines.size();
        for (int i = 0; i < len; i ++) {
            FMTFile x = Test.onlines.get(i);
            for (int j = 0; j < x.files.size(); j ++){
                Fm xx = x.files.get(j);
                if (xx.name.equals(substring)){
                    list.add(xx.toString());
                }
            }
        }
        return list;
    }
}