package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import java.net.Socket;

public class SimpleDhtProvider extends ContentProvider {
    private static final int SERVER_PORT=10000;
    private static Uri mUri;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private static final String TAG= SimpleDhtProvider.class.getSimpleName();
    private static int myPort=0;
    private static String portStr="";
    private static int successorPort=-2;
    private static int predecesssorPort=-2;
    private static String NodeId="";
    private static String successorId="invalid";
    private static String predecesssorId="invalid";
    public static MatrixCursor cursor;
    private static int[] REMOTE_PORTS ={11108, 11112, 11116, 11120, 11124};
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private static boolean waitLoop= false;
    private static boolean standAloneNode= true;
    private static boolean waitForStability= true;
    private static ArrayList<ChordNode> ChordRing=new ArrayList<ChordNode>();
    //take a hashmap here, port no vs


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        Log.v(TAG,"Entered delete() with selection"+selection);
        String message= "delete"+"_"+selection;
        if ( (selection.equals("@")) || (selection.equals("*")) )
        {
            int succ=dbHelper.deleteAllMessage();
            Log.v(TAG,"Deleting all messages success:"+ succ);
            if (selection.equals("*") )
            {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message, "delete");
            }
            return 0;
        }
        String key=null;
        try {
            key=genHash(selection);
        }catch (Exception e) {
            Log.e(TAG,e.toString());
        }

        //JFT:REMOVE AFTER TESTING IS DONE!!start
        Log.v(TAG,"NodeID:"+ NodeId);
        Log.v(TAG,"key:"+ key);
        Log.v(TAG,"predecessorId:"+ predecesssorId);
        if (key.compareTo(NodeId)<= 0) {Log.v(TAG,"selection is less than equal to nodeid"); }
        if (key.compareTo(predecesssorId)>0) {Log.v(TAG,"selection is greater tha predId"); }
        if (NodeId.compareTo(predecesssorId)< 0) {Log.v(TAG,"Nodeid is less than predId"); }
        if (key.compareTo(predecesssorId)>0) {Log.v(TAG,"selection is greater than predid"); }
        if (key.compareTo(NodeId)<0) {Log.v(TAG,"selection is less than nodeid"); }
        //JFT--END

        if ( (predecesssorPort==-2) ||(predecesssorPort==-1) ||((key.compareTo(NodeId)<= 0) && (key.compareTo(predecesssorId)>0)) ||
                ((NodeId.compareTo(predecesssorId)< 0) &&  ( (key.compareTo(predecesssorId)>0)||(key.compareTo(NodeId)<0) ) ) )
        {

            int succ=dbHelper.deleteMessage(selection);
            Log.v(TAG,"Deleting success:"+ succ);

        }
        else
        {
            Log.v(TAG,"starting new client task");
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,"delete");

        }
        return 0;


    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        //write a function that does chord ring protocol and call it from here. This function will return success
        //after key value pair is successfully inserted.Means this function should wait till it gets Ack?
        /*if(waitForStability)
        {
            try {
                Thread.sleep(2000);
            }catch (Exception e)
            {
                Log.e(TAG, e.toString());
            }
            waitForStability=false;

        }*/
        Log.v(TAG,"Entered insert()");
        String message= "insert"+"_"+values.get("key").toString()+"_"+values.get("value").toString();
        String key=values.get("key").toString();
        //after hashing key
        try {
            key=genHash(values.get("key").toString());
        }catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        Log.v(TAG,"message is"+message);
        //JFT:START
        Log.v(TAG,"key:"+values.get("key").toString());
        Log.v(TAG,"NodeId:"+NodeId);
        Log.v(TAG,"PredecessorId:"+predecesssorId);
        Log.v(TAG,"SuccessorId:"+successorId);
        Log.v(TAG,values.get("value").toString());
        if (key.compareTo(NodeId)<= 0) {Log.v(TAG,"key is less than equal to nodeid"); }
        if (key.compareTo(predecesssorId)>0) {Log.v(TAG,"key is greater tha predId"); }
        if (NodeId.compareTo(predecesssorId)< 0) {Log.v(TAG,"Nodeid is less than predId"); }
        if (key.compareTo(predecesssorId)>0) {Log.v(TAG,"key is greater than predid"); }
        if (key.compareTo(NodeId)<0) {Log.v(TAG,"key is less than nodeid"); }
        //JFT--END

        if ((predecesssorPort==-2) ||(predecesssorPort==-1)||((key.compareTo(NodeId)<= 0) && (key.compareTo(predecesssorId)>0)) ||
                    ((NodeId.compareTo(predecesssorId)< 0) &&  ( (key.compareTo(predecesssorId)>0)||(key.compareTo(NodeId)<0) ) ) )
        {
            boolean success=false;
            success=dbHelper.insertkey(values);
            Log.v(TAG,"Inserting success"+ success);

        }
        else
        {
            Log.v(TAG,"starting new client task");
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,"insert");

        }
        return uri;


    }



    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        Log.v(TAG,"Entered OnCreate()");

        dbHelper = new DBHelper(this.getContext());
        db= dbHelper.getWritableDatabase();

        //Getting port of avd
        TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = (Integer.parseInt(portStr) * 2);
        Log.v(TAG,"my port is:"+myPort);
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");

        //Get NodeId of this node
        try {
            NodeId=genHash(portStr);
        }catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        Log.v(TAG,"my nodeid is:"+NodeId);
        //create a server socket that keeps listening
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket" + e);
            //return;
        }

        NodeJoinRequest();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        //Write a function that checks if it is present in own files if not forwards it to the successor.
        /*if (sortOrder.equals("Originator"))
        {
            sortOrder=Integer.toString(myPort);
        }*/
        String[] columnNames = {
                "key", "value"
        };
        cursor = new MatrixCursor(columnNames);
        Cursor res= null;
        String key="";
        try {
            key=genHash(selection);
        }catch (Exception e) {
            Log.e(TAG,e.toString());
        }
        String message="query"+"_"+selection+"_"+myPort;
        Log.v(TAG, "Entering query() for selection:" + selection);
        if (selection.equals("@"))
        {
            res=dbHelper.getAllMessages();
        }
        else if (selection.equals("*"))
        {
            res=dbHelper.getAllMessages();
             GlobalQueryHandler();
              while (!waitLoop)
              {

              }
              waitLoop = false;
              res=cursor;
              Log.v(TAG, "Returning cursor!");

        }
        else
        {
            //JFT--START
            if (key.compareTo(NodeId)<= 0) {Log.v(TAG,"key is less than equal to nodeid"); }
            if (key.compareTo(predecesssorId)>0) {Log.v(TAG,"key is greater tha predId"); }
            if (NodeId.compareTo(predecesssorId)< 0) {Log.v(TAG,"Nodeid is less than predId"); }
            if (key.compareTo(predecesssorId)>0) {Log.v(TAG,"key is greater than predid"); }
            if (key.compareTo(NodeId)<0) {Log.v(TAG,"key is less than nodeid"); }
            //JFT--END

            if ( (predecesssorPort==-2)||(predecesssorPort==-1)||((key.compareTo(NodeId)<= 0) && (key.compareTo(predecesssorId)>0)) ||
                    ((NodeId.compareTo(predecesssorId)< 0) &&  ( (key.compareTo(predecesssorId)>0)||(key.compareTo(NodeId)<0) ) ) )
            {
                res=dbHelper.getMessage(selection);
                Log.v(TAG,"Query successful");

            }
            else
            {

                Log.v(TAG,"starting new client task for query");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,message,"query");
                //wait here
                //and return from global variable
                Log.v(TAG,"Waiting till query reply is received!");
                while (!waitLoop)
                {

                }
                waitLoop = false;
                res=cursor;
                Log.v(TAG,"Returning cursor!");
            }
        }

        return res;
    }

    private void GlobalQueryHandler()
    {
      //get from own
       Log.v(TAG,"Entered GlobalQueryHandler() ");
       Cursor res=dbHelper.getAllMessages();
       //copying to cursor
       if (res.moveToFirst()) {
            do {
                String var1 = res.getString(res.getColumnIndex("key"));
                String var2 = res.getString(res.getColumnIndex("value"));

                String[] row = {
                        var1, var2
                };
                cursor.addRow(row);

            } while (res.moveToNext());
        }
        res.close();
        Log.v(TAG,"starting new client task for global query-multicasting");
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, null, "globalquery");
        Log.v(TAG, "Exit GlobalQueryHandler() ");


    }

    public void queryForwardAndReply(String input) {
        // TODO Auto-generated method stub

        //String message="query"+"_"+selection+"_"+myPort;
        Log.v(TAG, "Entering queryForwardAndReply() for input:" + input);
        String[] inputvalues = input.split("_");
        String selection=inputvalues[1];
        String remoteport=inputvalues[2];
        String key="";
        try {
            key=genHash(selection);
        }catch (Exception e) {
            Log.e(TAG,e.toString());
        }
            //JFT--START
        Log.v(TAG,"key:"+key);
        Log.v(TAG,"NodeId:"+NodeId);
        Log.v(TAG,"PredecessorId:"+predecesssorId);
        Log.v(TAG,"SuccessorId:"+successorId);
        if (key.compareTo(NodeId)<= 0) {Log.v(TAG,"key is less than equal to nodeid"); }
        if (key.compareTo(predecesssorId)>0) {Log.v(TAG,"key is greater tha predId"); }
        if (NodeId.compareTo(predecesssorId)< 0) {Log.v(TAG,"Nodeid is less than predId"); }
        if (key.compareTo(predecesssorId)>0) {Log.v(TAG,"key is greater than predid"); }
        if (key.compareTo(NodeId)<0) {Log.v(TAG, "key is less than nodeid"); }
            //JFT--END
       try {
           if ((predecesssorPort == -1) || ((key.compareTo(NodeId) <= 0) && (key.compareTo(predecesssorId) > 0)) ||
                   ((NodeId.compareTo(predecesssorId) < 0) && ((key.compareTo(predecesssorId) > 0) || (key.compareTo(NodeId) < 0)))) {
               //Convert cursor to string and send it to the port
               Cursor res = dbHelper.getMessage(selection);
               String cursorstring = CursorTostring(res);
               Log.v(TAG, "Forwarding to the remote avd");
               Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remoteport));
               PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
               out.println(cursorstring);
               Log.v(TAG, "Query successful");

           } else {   //forward it to the successor
               Log.v(TAG, "Forwarding to the successor"+successorId);
               Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), successorPort);
               PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
               out.println(input);

           }
       }catch (Exception e)
       {
           Log.e(TAG,e.toString());

       }


    }
   private String CursorTostring(Cursor res) {
       Log.v(TAG,"Entered CursorTostring ");
       String output="queryresponse";

       if (res.moveToFirst()) {
           do {
               String var1 = res.getString(res.getColumnIndex("key"));
               String var2 = res.getString(res.getColumnIndex("value"));
               output = output + "_" + var1 + "_" + var2;

           } while (res.moveToNext());
       }
       res.close();
       Log.v(TAG,"CursorToString output is:"+output);
       return output;

   }
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while (true) {
                try {
                    //only for ping request type
                    Log.v(TAG, "Entered servertask");
                    Socket socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out=new PrintWriter(socket.getOutputStream(),true);
                    String input=in.readLine();
                    Log.v(TAG, "ServerTask received message:"+input);
                    String[] inputvalues=input.split("_");
                    Log.v(TAG, "SeverTask Request received"+inputvalues[0]);

                    if (inputvalues[0].equals("nodejoin")){
                        NodeJoinRequestManager(input);
                    }
                    else if (inputvalues[0].equals("nodejoinresponse")){
                        //output="nodejoinresponse"+"_"+newnodesuccid+"_"+newnodesuccport+"_"+newnodepredid+"_"+newnodepredport;
                        successorId=inputvalues[1];
                        successorPort=Integer.parseInt(inputvalues[2]);
                        predecesssorId=inputvalues[3];
                        predecesssorPort=Integer.parseInt(inputvalues[4]);
                        String logmsg="Setting successor and predecessor:"+"successorId:"+successorId+"successorPort:"+
                                successorPort+"predecesssorId:" +  predecesssorId+"predecesssorPort:"+predecesssorPort+"Nodeid:"+NodeId+"myPort"+myPort;
                        Log.v(TAG,logmsg);
                    }
                    else if (inputvalues[0].equals("NewSucc")){
                        //"NewSucc" + "_" + newnodeid + "_" + newnodeport;
                        successorId=inputvalues[1];
                        successorPort=Integer.parseInt(inputvalues[2]);
                        String logmsg="Changing successor :"+"successorId:"+successorId+"successorPort:"+
                                successorPort+"predecesssorId:" +  predecesssorId+"predecesssorPort:"+predecesssorPort+"Nodeid:"+NodeId+"myPort"+myPort;
                        Log.v(TAG,logmsg);

                    } else if (inputvalues[0].equals("NewPred")){
                        //"NewPred" + "_" + newnodeid + "_" + newnodeport;
                        predecesssorId=inputvalues[1];
                        predecesssorPort=Integer.parseInt(inputvalues[2]);
                        String logmsg="Changing predecessor :"+"successorId:"+successorId+"successorPort:"+
                                successorPort+"predecesssorId:" +  predecesssorId+"predecesssorPort:"+predecesssorPort+"Nodeid:"+NodeId+"myPort"+myPort;
                        Log.v(TAG,logmsg);

                    }
                    else if (inputvalues[0].equals("IPSpaceRePartition")){
                        //lookup--if matched ping the node , else forward in the ring
                        processIDspace(input);
                    }
                    else if (inputvalues[0].equals("insert")){
                        Log.v(TAG, "ServerTask Insert:"+inputvalues[1]+inputvalues[2]);
                        ContentValues contentvalues = new ContentValues();
                        contentvalues.put(KEY_FIELD, inputvalues[1] );
                        contentvalues.put(VALUE_FIELD, inputvalues[2]);
                        insert(mUri, contentvalues);

                    }//for chord protocol of delete
                    else if(inputvalues[0].equals("delete")) {
                        Log.v(TAG, "ServerTask Delete:" + inputvalues[1]);
                        delete(mUri, inputvalues[1], null);

                    }
                    else if(inputvalues[0].equals("query")){
                        Log.v(TAG, "ServerTask Query:"+inputvalues[1]);
                        queryForwardAndReply(input);

                    }
                    else if(inputvalues[0].equals("globalquery"))
                    {
                        Log.v(TAG, "Globalquery");
                        //get input as string
                        Cursor res=dbHelper.getAllMessages();
                        String output=CursorTostring(res);
                        out.println(output);


                    }
                    else if(inputvalues[0].equals("queryresponse")){
                        Log.v(TAG, "ServerTask QueryResponse:");
                        //Construct a Cursor from input and return in global object
                        CursorFromString(input);
                        //cursor=cr;
                        waitLoop=true;
                        //set waitLoop to true

                    }
                    else{

                        Log.v(TAG, "didn't match!");
                    }


                } catch (IOException e) {
                    Log.e(TAG, "ServerTask IOException");

                }

            }

        }

        protected void onProgressUpdate(String...strings) {


        }


    }

    private void NodeJoinRequest(){

        if (portStr.equals("5554") ) {
            //Do nothing,set successor and predecessor as null
            successorId="null";
            successorPort=-1;
            predecesssorId="null";
            predecesssorPort=-1;
            Log.v(TAG, "Initializing 5554, setting successors and predecessors for 5554");
            //Initialize arraylist and put this node in it
            ChordNode node=new ChordNode(NodeId,myPort,successorId,successorPort,predecesssorId,predecesssorPort);
            ChordRing.add(node);
            ChordNode node1=ChordRing.get(0);
            String logmsg="Setting node: "+"NodeId"+node1.NodeId+"successorId:"+node1.successorId+"successorPort"+node1.successorPort+
                    "predecessorId"+node1.predecesssorId+"predecessorPort"+node1.predecesssorPort;
            Log.v(TAG,logmsg);
        }
        else {

            Log.v(TAG,"Setting successor and predecessor for other node"+ portStr);
            try {
                Log.v(TAG, "Sending Ping-Nodejoin request to 5554");
                String ping = "nodejoin" + "_" + NodeId + "_" + myPort;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ping, "nodejoin");



            }catch (Exception e) {
                Log.e(TAG,e.toString());
            }


        }

    }

    private void NodeJoinRequestManager(String input)
    {
        String output="";
        String[] inputvalues=input.split("_");
        //JFT //String ping = "nodejoin" + "_" + NodeId + "_" + myPort;
        Log.v(TAG,"EnteredNodeJoinRequestManager() input values are"+inputvalues[1]+"_"+inputvalues[2]);
        String newnodeid=inputvalues[1];
        int newnodeport=Integer.parseInt(inputvalues[2]);
        for(int i=0;i<ChordRing.size();i++)
        {
            ChordNode node=ChordRing.get(i);
            String logmsg="Iterating ChordRing node: "+"NodeId"+node.NodeId+"successorId:"+node.successorId+"successorPort"+node.successorPort+
                    "predecessorId"+node.predecesssorId+"predecessorPort"+node.predecesssorPort;
            Log.v(TAG,logmsg);

            //JFTs
            if (node.successorPort==-1){ Log.v(TAG,"successorport is -1 i.e only one node in the ring");}
            if (newnodeid.compareTo(node.NodeId)<= 0){ Log.v(TAG,"key is smaller than nodeId");}
            if (newnodeid.compareTo(node.predecesssorId)>0){ Log.v(TAG,"key is greater than predId");}
            if (node.NodeId.compareTo(node.predecesssorId)< 0){ Log.v(TAG,"nodeid is smaller than predId");}
            if  (newnodeid.compareTo(node.predecesssorId)>0){Log.v(TAG,"key greater than predId");}
            if  (newnodeid.compareTo(node.NodeId)<0){Log.v(TAG,"key less than NodeId");}
            if  ( (node.successorPort==-1) ||  ((newnodeid.compareTo(node.NodeId)<= 0) && (newnodeid.compareTo(node.predecesssorId)>0)) ||
                    ((node.NodeId.compareTo(node.predecesssorId)< 0) &&  ( (newnodeid.compareTo(node.predecesssorId)>0)||(newnodeid.compareTo(node.NodeId)< 0) ) ) )
            {
                //we found the node--sending to the node
                //adding node to the ring
                String newnodesuccid=node.NodeId;
                int newnodesuccport=node.myPort;
                String newnodepredid=node.predecesssorId;
                int newnodepredport=node.predecesssorPort;

                if(node.successorPort==-1)
                {
                  Log.v(TAG,"Forming ring of two nodes--changing pred of new node");
                  newnodepredid=node.NodeId;
                  newnodepredport=node.myPort;
                }
                ChordNode newnode=new ChordNode(newnodeid,newnodeport,newnodesuccid,newnodesuccport,newnodepredid,newnodepredport);
                output="nodejoinresponse"+"_"+newnodesuccid+"_"+newnodesuccport+"_"+newnodepredid+"_"+newnodepredport;
                try{
                    Log.v(TAG,"Sending initial message to port:"+newnodeport+":"+output);
                    Socket socket=new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),newnodeport);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(output);
                    out.close();
                    socket.close();
                }catch (Exception e)
                {
                    Log.e(TAG,e.toString());
                }
                //Change predecessor and successor in the ring and tell the nodes to change their successor and pred
                //first search the predecessor of the new node and change its successor.
                if(node.successorPort==-1)
                {
                    Log.v(TAG,"Forming ring of two nodes--changing 5554 successor and predecessor");
                    node.successorPort=newnodeport;
                    node.successorId=newnodeid;
                    node.predecesssorPort=newnodeport;
                    node.predecesssorId=newnodeid;
                    //change actual config too
                    successorPort=newnodeport;
                    successorId=newnodeid;
                    predecesssorPort=newnodeport;
                    predecesssorId=newnodeid;

                }
                else
                {
                    //Search the pred of the node in the ring and change its succesor to new node and its port
                    for(int j=0;j<ChordRing.size();j++) {
                        ChordNode prednode = ChordRing.get(j);
                        if (prednode.NodeId.equals(newnodepredid)) {
                            output = "NewSucc" + "_" + newnodeid + "_" + newnodeport;
                            try {
                                Log.v(TAG, "Sending NewSucc message to port:" + prednode.myPort + ":" + output);
                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), prednode.myPort);
                                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                                out.println(output);
                                out.close();
                                socket.close();
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                            //Change it in the ring now
                            prednode.successorId = newnodeid;
                            prednode.successorPort = newnodeport;
                            break;

                        }
                    }
                    //Search the succ of the node in the ring(we have it already!) and change its succesor to new node and its port
                    output = "NewPred" + "_" + newnodeid + "_" + newnodeport;
                    try {
                        Log.v(TAG, "Sending NewPred message to port:" + node.myPort + ":" + output);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), node.myPort);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(output);
                        out.close();
                        socket.close();
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                    //change it in the ChordRing now:
                    node.predecesssorId=newnodeid;
                    node.predecesssorPort=newnodeport;

                }//end--else successorport==-1
                //add node in the chordring
                Log.v(TAG, "adding newnode in the ChordRing");
                ChordRing.add(newnode);
                break;



            }//end of if--we found the node



        }//end of loop where we find node , do processing and break after done
        for(int k=0;k<ChordRing.size();k++)
        {
            ChordNode node1=ChordRing.get(k);
            String logmsg="12345Iterating ChordRing node: "+"NodeId"+node1.NodeId+"successorId:"+node1.successorId+"successorPort"+node1.successorPort+
                    "predecessorId"+node1.predecesssorId+"predecessorPort"+node1.predecesssorPort;
            Log.v(TAG,logmsg);

        }

    }

    private void NodeJoinResponseManager(String input)
    {
        Log.v(TAG,"Entered NodeJoinResponseManager() for input"+input);
        //output="nodejoinresponse"+"_"+NodeId+"_"+myPort+"_"+predecesssorId+"_"+predecesssorPort+"_"+successorId+"_"+successorPort;
        String[] inputvalues=input.split("_");
        String nodeid=inputvalues[1];
        String nodeport=inputvalues[2];
        String predecesssorid=inputvalues[3];
        String predecesssorport=inputvalues[4];
        //Whatever nodeid we have got,it should be successor of joining node,its predecessor should be predessor of
        //joining node, and tell that node to change its predecessor node
        Log.v(TAG,"Changing successorid to:"+nodeid);
        successorId=nodeid;
        successorPort=Integer.parseInt(nodeport);
        if (Integer.parseInt(predecesssorport)==-1)
        {
            predecesssorId=nodeid;
            predecesssorPort=Integer.parseInt(nodeport);
            Log.v(TAG,"Setting prdecessor in case of only 2 nodes in the ring");
        }
        else {
            predecesssorId = predecesssorid;
            predecesssorPort = Integer.parseInt(predecesssorport);
            Log.v(TAG,"Setting prdecessor in case there are more than two nodes in the ring");
        }
        Log.v(TAG, "nodeid:"+NodeId);
        String logmsg="Setting successorId:"+successorId+" successorPort:"+successorPort+" predecesssorId:"+predecesssorId+" predecesssorPort:"+
                predecesssorPort;
        Log.v(TAG,logmsg);



    }

    private void CursorFromString(String input){
        String[] inputvalues=input.split("_");
        String selection=inputvalues[1];
        String value=inputvalues[2];
        String[] row = {
                selection, value
        };
        cursor.addRow(row);

    }

    private void CursorFromStringGlobal(String input){
        String[] inputvalues=input.split("_");
        int i=1;
        while(i<inputvalues.length) {

            String[] row = {
                    inputvalues[i], inputvalues[i+1]
            };
            cursor.addRow(row);
            i=i+2;
        }

    }

    private String IDSpaceRePartition()
    {
        String output="IPSpaceRePartition";
        Cursor res=dbHelper.getPartitionMessage(predecesssorId);
        if(res.moveToFirst()){
            do{
                String var1 = res.getString(res.getColumnIndex("key"));
                String var2 = res.getString(res.getColumnIndex("value"));
                output=output+"_"+var1+"_"+var2+"_";

            }while (res.moveToNext());
        }
        res.close();
        int succ=dbHelper.DeleteMessagePartition(predecesssorId);
        Log.v(TAG,"Output of IDSpaceRePartition(): "+output);
        return output;


    }

   /* private void NodeJoinRequest(){

        if (portStr.equals("5554") ) {
            //Do nothing,set successor and predecessor as null
            successorId="null";
            successorPort=-1;
            predecesssorId="null";
            predecesssorPort=-1;
            Log.v(TAG,"Initializing 5554, setting successors and predecessors for 5554");
            try {
                Thread.sleep(2000);
            }catch (Exception e)
            {
                Log.e(TAG,e.toString());
            }
        }
        else {

            Log.v(TAG,"Setting successor and predecessor for other node"+ portStr);
            try {
                Log.v(TAG, "Sending Ping-Nodejoin request to 5554");
                String ping = "nodejoin" + "_" + myPort + "_" + NodeId;
                //String ping1 = "ping" + "_" + myPort + "_" + NodeId;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,ping,"nodejoin");
                Thread.sleep(2000);


            }catch (Exception e) {
                Log.e(TAG,e.toString());
            }


        }

    }

    private void NodeJoinRequestManager(String input)
    {
        String output="";
        String[] inputvalues=input.split("_");
        //JFT
        Log.v(TAG,"input values are"+inputvalues[1]+"_"+inputvalues[2]);
        Log.v(TAG,"NodeId"+NodeId);
        Log.v(TAG,"Predecessor"+predecesssorId);
        try{
            if (successorPort==-1){ Log.v(TAG,"successorport is -1 i.e only one node in the ring");}
            if (inputvalues[2].compareTo(NodeId)<= 0){ Log.v(TAG,"key is smaller than nodeId");}
            if (inputvalues[2].compareTo(predecesssorId)>0){ Log.v(TAG,"key is greater than predId");}
            if (NodeId.compareTo(predecesssorId)< 0){ Log.v(TAG,"nodeid is smaller than predId");}
            if  (inputvalues[2].compareTo(predecesssorId)>0){Log.v(TAG,"key greater than predId");}
            if  (inputvalues[2].compareTo(NodeId)<0){Log.v(TAG,"key less than NodeId");}
            if  ( (successorPort==-1) ||  ((inputvalues[2].compareTo(NodeId)<= 0) && (inputvalues[2].compareTo(predecesssorId)>0)) ||
                    ((NodeId.compareTo(predecesssorId)< 0) &&  ( (inputvalues[2].compareTo(predecesssorId)>0)||(inputvalues[2].compareTo(NodeId)< 0) ) ) )
            {
                //String ping = "nodejoin" + "_" + myPort + "_" + NodeId;//This is a two node in a ring case
                output="nodejoinresponse"+"_"+NodeId+"_"+myPort+"_"+predecesssorId+"_"+predecesssorPort+"_"+successorId+"_"+successorPort;
                Log.v(TAG, "Sending response to joining node:"+inputvalues[1]+" :message"+output);
                Socket socket=new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),Integer.parseInt(inputvalues[1]));
                //BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(output);
                //output=in.readLine();
                //Log.v(TAG, "Ack received from pred node:" + output);
                //in.close();
                out.close();
                socket.close();


                //tell predecessor to change successor
                if(successorPort!=-1) {
                    Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), predecesssorPort);
                    PrintWriter out1 = new PrintWriter(socket1.getOutputStream(), true);
                    //BufferedReader in1=new BufferedReader(new InputStreamReader(socket1.getInputStream()));
                    output = "NodeJoinNewSucc" + "_" + inputvalues[1] + "_" + inputvalues[2];
                    out1.println(output);
                    Log.v(TAG, "Sending response to pred node:" + predecesssorPort + ":message " + output);
                    //output=in1.readLine();
                    //Log.v(TAG, "Ack received from pred node:" + output);
                    //in1.close();
                    out1.close();
                    socket1.close();

                }


                //change predecessor
                predecesssorPort=Integer.parseInt(inputvalues[1]);
                predecesssorId=inputvalues[2];
                if(successorPort==-1) {
                    successorPort=Integer.parseInt(inputvalues[1]);
                    Log.v(TAG,"Changing successorid to:"+inputvalues[2]);
                    successorId=inputvalues[2];
                }
                Log.v(TAG, "nodeid:"+NodeId);
                String logmsg="Setting successorId:"+"succId:"+successorId+"succPort:"+successorPort+"predId:"+predecesssorId+"predPort:"+predecesssorPort;
                Log.v(TAG, logmsg);

                //send ID space and delete
                output=IDSpaceRePartition();
                Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),predecesssorPort);
                PrintWriter out2= new PrintWriter(socket2.getOutputStream(),true);
                out2.println(output);

                out2.close();
                socket2.close();


            }
            else {
                Log.v(TAG, "Forwarding the request to successor"+successorPort);
                Socket socket=new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),successorPort);
                PrintWriter out=new PrintWriter(socket.getOutputStream(),true);
                out.println(input);


            }
        }catch (Exception e)
        {
            Log.e(TAG,e.toString());
        }



    }

    private void NodeJoinResponseManager(String input)
    {
       Log.v(TAG,"Entered NodeJoinResponseManager() for input"+input);
       //output="nodejoinresponse"+"_"+NodeId+"_"+myPort+"_"+predecesssorId+"_"+predecesssorPort+"_"+successorId+"_"+successorPort;
       String[] inputvalues=input.split("_");
       String nodeid=inputvalues[1];
       String nodeport=inputvalues[2];
       String predecesssorid=inputvalues[3];
       String predecesssorport=inputvalues[4];
        //Whatever nodeid we have got,it should be successor of joining node,its predecessor should be predessor of
        //joining node, and tell that node to change its predecessor node
        Log.v(TAG,"Changing successorid to:"+nodeid);
        successorId=nodeid;
        successorPort=Integer.parseInt(nodeport);
        if (Integer.parseInt(predecesssorport)==-1)
        {
            predecesssorId=nodeid;
            predecesssorPort=Integer.parseInt(nodeport);
            Log.v(TAG,"Setting prdecessor in case of only 2 nodes in the ring");
        }
        else {
            predecesssorId = predecesssorid;
            predecesssorPort = Integer.parseInt(predecesssorport);
            Log.v(TAG,"Setting prdecessor in case there are more than two nodes in the ring");
        }
        Log.v(TAG, "nodeid:"+NodeId);
        String logmsg="Setting successorId:"+successorId+" successorPort:"+successorPort+" predecesssorId:"+predecesssorId+" predecesssorPort:"+
                predecesssorPort;
        Log.v(TAG,logmsg);



    }*/



    private class ClientTask extends AsyncTask<String,Void,Void>{

        @Override
        protected Void doInBackground(String... msgs ){

        try{
            //for message type ping only--different for message type insert,delete and query
            if  (msgs[1].equals("nodejoin")) {
                Log.v(TAG, "Entered ClientTask for ping-nodejoin request");
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), 11108);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Log.v(TAG, "ClientTask sending message:"+msgs[0]);
                out.println(msgs[0]);
                out.close();
                socket.close();

            }
            else if  (msgs[1].equals("globalquery")) {
                //multicast
                for(int i=0;i<5;i++)
                {   if(REMOTE_PORTS[i]!=myPort)
                   {
                      try {

                         Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                REMOTE_PORTS[i]);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                         out.println("globalquery");
                         String output = in.readLine();
                         //call function which adds output to cursor
                         CursorFromStringGlobal(output);
                         socket.close();

                      } catch (Exception e) {
                         Log.e(TAG, e.toString());

                      }
                   }

                }
                waitLoop=true;


            }
            else  {
                Log.v(TAG, "Entered client task for Insert,Delete,Query request");
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), successorPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                //BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.v(TAG, msgs[0]);
                out.println(msgs[0]);
                /*if((msgs[1].equals("query"))
                {
                    String queryresponse=in.readLine();
                    //build cursor object from response and write it in global variable and set flags
                    boolean waitflag=false;
                    boolean received=false;
                }*/
                //in.close();
                out.close();
                socket.close();
            }


        }catch (NullPointerException e){
            Log.e(TAG,Log.getStackTraceString(e));
        } catch (Exception e){
            Log.e(TAG,e.toString());
        }

        return null;


    }

}



    private void processIDspace(String input){
        Log.v(TAG,"Entered processIDspace for input:"+ input);

        String[] inputvalues=input.split("_");
        Log.v(TAG,Integer.toString(inputvalues.length));
        int i=1;
        //JFT--LOOP
        Log.v(TAG,"START while");
        while (i<inputvalues.length)
        {
            Log.v(TAG,"input"+i+":"+inputvalues[i]+"_"+inputvalues[i+1]);
            i=i+2;
        }
        Log.v(TAG,"END while");
        i=1;
        while (i<inputvalues.length)
        {
           ContentValues contentvalues = new ContentValues();
           contentvalues.put(KEY_FIELD, "key" +inputvalues[i] );
           contentvalues.put(VALUE_FIELD, "val" + inputvalues[i+1]);
           insert(mUri, contentvalues);
           i=i+2;
        }
        return;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }


}
