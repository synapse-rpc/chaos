## 西纳普斯 - synapse (Java Version)

### 此为系统核心交互组件,包含了事件和RPC系统

### 依赖包
1. amqp-client-4.0.2 
> http://central.maven.org/maven2/com/rabbitmq/amqp-client/4.0.2/amqp-client-4.0.2.jar
2. fastjson-1.2.46
> http://repo1.maven.org/maven2/com/alibaba/fastjson/1.2.46/fastjson-1.2.46.jar
3. slf4j-api-1.7.25
> http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar
4. slf4j-nop-1.7.25
> http://central.maven.org/maven2/org/slf4j/slf4j-nop/1.7.25/slf4j-nop-1.7.25.jar
5. commons-cli-1.4  (测试程序部分)
> http://mirrors.hust.edu.cn/apache//commons/cli/binaries/commons-cli-1.4-bin.zip

#### 安装包:
1. 下载 Jar 包(已经包含所有依赖)
> 还没打包出来
2. 用Maven/gradle安装
> 还没上传呢

#### 使用前奏:
1. 需要一个RabbitMQ服务器

#### 使用方式:
```java
package rpc.synapse.test;

import com.alibaba.fastjson.JSONObject;
import rpc.synapse.chaos.Synapse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Test {
    public static void main(String[] args) throws Exception {
        Synapse app = new Synapse();
        app.debug = true;
        app.mqHost = "xxx";
        app.mqUser = "xxx";
        app.mqPass = "xxx";
        app.sysName = "xxx";
        app.appName = "xxx";
        app.eventCallback = new EventTest();
        app.rpcCallback = new RpcTest();
        app.serve();
        String[] inputs;
        JSONObject body;
        JSONObject res;
        while (true) {
            body = new JSONObject();
            String str = readDataFromConsole("input >> ");
            inputs = str.split(" ");
            switch (inputs[0]) {
                case "event":
                    body.put("msg", inputs[2]);
                    app.sendEvent(inputs[1], body);
                    break;
                case "rpc":
                    body.put("msg", inputs[3]);
                    res = app.sendRpc(inputs[1], inputs[2], body);
                    System.out.println(res.toJSONString());
                    break;
                case "exit":
                    System.out.println("Bye Bye!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("unknow command!");
                    break;
            }
        }
    }

    private static String readDataFromConsole(String prompt) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String str = null;
        try {
            System.out.print(prompt);
            str = br.readLine();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }
}
```

#### CallBack类说明:
callback类 需要继承 BaseCallback , regAlias方法 key 表示 调用/监听 , value 表示对应执行的本类方法;
注意: 不支持#和*通配符,使用通配符可以拿到消息,但是目前无法匹配处理方法
```java
// BaseCallback.java
package rpc.synapse.chaos;

import java.util.Hashtable;

public class BaseCallback {
    //注册监听/调用方法
    public Hashtable<String, String> regAlias() {
        return new Hashtable<>();
    }
    
    //事件回调方法
    //返回 true 系统将确认事件, 返回 false 系统将事件返回队列
    public boolean test(JSONObject body, AMQP.BasicProperties props) {
        System.out.printf("**收到EVENT: %s@%s %s\n", props.getType(), props.getReplyTo(), body.toJSONString());
        return true; 
    }
    
    //Rpc服务回调方法
    public JSONObject test(JSONObject body, AMQP.BasicProperties props) {
        System.out.printf("**RPC请求: %s@%s %s\n", props.getType(), props.getReplyTo(), body.toJSONString());
        JSONObject ret = new JSONObject();
        ret.put("from", "java");
        ret.put("m", body.get("msg"));
        ret.put("number", 5233);
        return ret; 
    }
}
```

#### 客户端方法说明:
1. 发送事件
> Synapse.sendEvent(String eventName, JSONObject param)

2. RPC请求
> Synapse.sendRpc(String app, String method, JSONObject param)

3. 控制台日志
> Synapse.Log(String desc, String level)

日志级别: Synapse.LOGINFO,Synapse.LOGWARN,Synapse.LOGDEBUG,Synapse.LOGERROR

#### 参数说明:
```java
    public String mqHost;                           //MQ服务器地址
    public int mqPort = 5672;                       //MQ端口
    public String mqUser;                           //MQ用户
    public String mqPass;                           //MQ用户密码
    public String mqVHost = "/";                    //MQ虚拟机
    public String sysName;                          //系统名称(都处于同一个系统下才能通讯)
    public String appName;                          //应用名(当前应用的名字,不能于其他应用重复)
    public String appId;                            //应用ID(支持分布式,不输入会每次启动自动随机生成)
    public boolean debug = false;                   //调试
    public boolean disableEventClient = false;      //禁用事件客户端
    public boolean disableRpcClient = false;        //禁用RPC客户端
    public int rpcTimeout = 3;                      //RPC请求超时时间(只针对客户端有效)
    public int eventProcessNum = 20;                //事件服务并发量
    public int rpcProcessNum = 20;                  //RPC服务并发量
    public BaseCallback rpcCallback;                //RPC处理类(不指定默认禁用Rpc Server)
    public BaseCallback eventCallback;              //Event处理类(不指定默认禁用Event Server)
```

