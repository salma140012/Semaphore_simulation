import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
//used to control access to common resource by multiple threads
  class  Semaphore {

  int size= 1;
  Queue<Device> DQ = new LinkedList<>();

  Semaphore() {
    size = 1;
  }

  Semaphore(int size) {
    this.size = size;
  }

  public synchronized void waiting(Device Dev) throws IOException, InterruptedException {
    size-=1;
    if (size < 0) {
      DQ.add(Dev);
      Dev.SaveToLog("- "+"("+Dev.getDName() +")"+""+"(" + Dev.getDType()+")" + " arrived and waiting");
      wait();
    } else {
      Dev.SaveToLog("- "+"("+Dev.getDName() +")"+""+"(" + Dev.getDType()+")" + " arrived");
    }
  }
  public synchronized void Release_Signal(Device Dev) {
    size+=1;
    if (size <= 0) {
      DQ.remove(Dev);
      notify();
    }
  }
}
  // that contains a list of connections and methods to occupy a connection and release a connection

 class Router {
    int MaxCon;
    Semaphore sema;
    ArrayList<Device> DList = new ArrayList<Device>(); 
    int[] Num_of_Connections;
    
    Router(int MaxCon) {
        this.MaxCon=MaxCon;
        sema = new Semaphore(MaxCon);
        Num_of_Connections=new int[MaxCon];
        for (int i = 0; i < MaxCon; i++) {
        	Num_of_Connections[i]=-1;
        }
    }
    public void SetArray(ArrayList<Device> devs) {
    	DList = devs;
    }
    public void AddConnection(Device dev){
        for (int i = 0; i < Num_of_Connections.length; i++) {
            if (Num_of_Connections[i]==-1){
            	Num_of_Connections[i]= dev.getIdNum();
                dev.setDPort(i+1);
                break;
            }
        }
    } 

    public void connect(Device dev) throws IOException, InterruptedException {
    	
        sema.waiting(dev);
        synchronized (DList){
        	DList.add(dev);
        }
        synchronized (Num_of_Connections){
            AddConnection(dev);
        }
        Network.Dequeue(dev);
    }
    
    public void RealeaseConnection(Device dev){
        for (int i = 0; i < Num_of_Connections.length; i++) {
            if (Num_of_Connections[i]== dev.getIdNum()){
            	Num_of_Connections[i]=-1;
                break;
            }
        }
    }

    public void disconnect(Device dev) {
        synchronized (DList){
        	DList.remove(dev);
        }
        synchronized (Num_of_Connections){
            RealeaseConnection(dev);
        }
        sema.Release_Signal(dev);
      
    }

 
    public void run() {
        for (int i = 0; i < DList.size(); i++)
            (new Thread(new Device(DList.get(i), this))).start();
    }
  
    static String out="";
        public void Write(String logtext) throws IOException {
            out+=logtext+System.lineSeparator();
            FileWriter Output=new FileWriter("log.txt");
            Output.write(out);
            Output.close();
        }
      
    }

/*represent different devices (threads) that can be connected to the router;
each device has its own name  and type  and it may perform three activities:
 connect, perform online activity and disconnect/logout*/
class Device extends Thread {
    String Name, type;
    Router r;
    static int ids=0;
    int id;
    private int port;
   
    Device(Device dev, Router r) {
        this.Name = dev.Name;
        this.type = dev.type;
        this.r = r;
    }

    Device(String Name, Router r, String type) {
        this.Name = Name;
        this.type=type;
        this.r = r;
        this.id=ids++;
       
    }
    public String getDName() {
        return Name;
    }

    public String getDType() {
        return type;
    }
    public int getIdNum() {
        return id;
    }
  
    public void setDPort(int port) {
        this.port = port;
      }
    public  int getDPort() {
        return port;
    }
    public void SaveToLog(String loginfo) throws IOException { 
  	   
        r.Write(loginfo);
    }

    public void connect() throws IOException {
    	 SaveToLog("- "+"Connection "+port+": "+ Name + " Log in");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
   
    public void disconnect() throws IOException {
        SaveToLog("- "+"Connection "+port+": "+ Name + " Logged out");
        r.disconnect(this);
    }

    public void PerformOnlineActivity() throws IOException {
        SaveToLog("- "+"Connection "+port+": "+ Name + " performs online activity");
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            r.connect(this);
            this.connect();
            this.PerformOnlineActivity();
            this.disconnect();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

//this class contains the main method in which the user is asked for two inputs.
public class Network {
	
public static ArrayList<Device> DQ = new ArrayList<>();
public static int QSize;
public static Router router;
public synchronized static void AddD(String name, String type) {
    Device dtemp =new Device(name,router,type);
    DQ.add(dtemp);
}
public synchronized static void Dequeue(Device Dev){
    DQ.remove(Dev);
}
public static void main(String[] args) {
	
	        String userin;
	        Scanner sc = new Scanner(System.in);
	        System.out.println("What is the number of WI-FI Connections?");
	        userin= sc.nextLine();
	        router = new Router(Integer.parseInt(userin));
	        System.out.println("What is the number of devices Clients want to connect?");
	        userin= sc.nextLine();
	        QSize = Integer.parseInt(userin);
	        for (int i = 0; i < QSize; i++) {
	        	userin=sc.nextLine();
	            String[] Tcline =userin.split(" ");
	            AddD(Tcline[0],Tcline[1]);
	        }

	        for (int i = 0; i < QSize; i++) {
	            DQ.get(i).start();
	        }
	        sc.close();
	    }
   
}