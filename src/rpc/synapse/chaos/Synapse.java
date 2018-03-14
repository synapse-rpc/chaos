package rpc.synapse.chaos;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Synapse {
    public String mqHost;
    public int mqPort = 5672;
    public String mqUser;
    public String mqPass;
    public String mqVHost = "/";
    public String sysName;
    public String appName;
    public String appId;
    public boolean debug = false;
    public boolean disableEventClient = false;
    public boolean disableRpcClient = false;
    public int rpcTimeout = 3;
    public int eventProcessNum = 20;
    public int rpcProcessNum = 20;
    public BaseCallback rpcCallback;
    public BaseCallback eventCallback;

    private Connection conn;
    private RpcClient rpcClient;
    private EventClient eventClient;

    public static final String LOGINFO = "Info";
    public static final String LOGDEBUG = "Debug";
    public static final String LOGWARN = "Warn";
    public static final String LOGERROR = "Error";

    public void serve() throws Exception {
        if (appName == null || sysName == null) {
            log("Must Set SysName and AppName system exit .", LOGERROR);
            System.exit(0);
        } else {
            log(String.format("System Name: %s", sysName), LOGINFO);
            log(String.format("App Name: %s", appName), LOGINFO);
        }
        if (appId == null) {
            appId = randomString();
        }
        log(String.format("App ID: %s", appId), LOGINFO);
        if (debug) {
            log("App Run Mode: Debug", LOGDEBUG);
        } else {
            log("App Run Mode: Production", LOGINFO);
        }
        createConnection();
        checkAndCreateExchange();
        //Event服务器
        if (eventCallback == null) {
            log("Event Server Disabled: eventCallback not set", LOGWARN);
        } else {
            new EventServer(this).run();
        }
        //Rpc服务器
        if (rpcCallback == null) {
            log("Rpc Server Disabled: rpcCallback not set", LOGWARN);
        } else {
            new RpcServer(this).run();
        }
        //Event客户端
        if (disableEventClient) {
            log("Event Client Disabled: disableEventClient set true", LOGWARN);
        } else {
            eventClient = new EventClient(this);
            log("Event Client Ready");
        }
        //Rpc客户端
        if (disableRpcClient) {
            log("Rpc Client Disabled: disableEventClient set true", LOGWARN);
        } else {
            rpcClient = new RpcClient(this);
            rpcClient.run();
            log(String.format("Rpc Client Timeout: %ds", rpcTimeout));
            log("Rpc Client Ready");
        }
    }

    public void sendEvent(String event, JSONObject param) throws Exception {
        if (disableEventClient) {
            log("Event Client Disabled: disableEventClient set true", LOGERROR);
        } else {
            eventClient.send(event, param);
        }
    }

    public JSONObject sendRpc(String app, String method, JSONObject param) throws Exception {
        JSONObject res;
        if (disableRpcClient) {
            res = new JSONObject();
            res.put("rpc_error", "rpc client disabled");
            log("Rpc Client Disabled: disableEventClient set true", LOGERROR);
        } else {
            res = rpcClient.send(app, method, param);
        }
        return res;
    }

    private void createConnection() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(mqHost);
        factory.setPort(mqPort);
        factory.setVirtualHost(mqVHost);
        factory.setUsername(mqUser);
        factory.setPassword(mqPass);
        conn = factory.newConnection();
        log("Rabbit MQ Connection Created.", LOGINFO);
    }

    public Channel createChannel(int processNum, String desc) throws Exception {
        Channel channel = conn.createChannel();
        log(String.format("%s Channel Created", desc), LOGINFO);
        if (processNum > 0) {
            channel.basicQos(processNum);
            log(String.format("%s MaxProcessNum: %d", desc, processNum), LOGINFO);
        }
        return channel;
    }

    private void checkAndCreateExchange() throws Exception {
        Channel channel = createChannel(0, "Exchange");
        channel.exchangeDeclare(sysName, "topic", true, true, null);
        log("Register Exchange Successed.");
        channel.close();
        log("Exchange Channel Closed");
    }

    public static void log(String desc, String level) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = dateFormat.format(new Date());
        System.out.printf("[%s][Synapse %s] %s\n", timeStr, level, desc);
    }

    public static void log(String desc) {
        log(desc, LOGINFO);
    }

    public static String randomString() {
        int length = 20;
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; ++i) {
            int number = random.nextInt(35);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
