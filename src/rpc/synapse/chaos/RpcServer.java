package rpc.synapse.chaos;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;

public class RpcServer {
    private Synapse s;
    private Channel ch;
    private String queueName;
    private String router;
    private Hashtable<String, String> alias;

    RpcServer(Synapse synapse) throws Exception {
        s = synapse;
        ch = s.createChannel(s.eventProcessNum, "RpcServer");
        queueName = String.format("%s_%s_server", s.sysName, s.appName);
        router = String.format("server.%s", s.appName);
        alias = s.rpcCallback.regAlias();
    }

    private void checkAndCreateQueue() throws Exception {
        ch.queueDeclare(queueName, true, false, true, null);
        ch.queueBind(queueName, s.sysName, router);
        Enumeration keys = alias.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            Synapse.log(String.format("*RPC %s -> %s", key, alias.get(key)));
        }
    }

    public void run() throws Exception {
        checkAndCreateQueue();
        Consumer consumer = new DefaultConsumer(ch) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                JSONObject content = JSON.parseObject(new String(body, "UTF-8"));
                if (s.debug) {
                    Synapse.log(String.format("Rpc Receive: (%s)%s@%s->%s %s", properties.getMessageId(), properties.getType(), s.appName, properties.getReplyTo(), content.toJSONString()), Synapse.LOGDEBUG);
                }
                JSONObject res = new JSONObject();
                if (alias.containsKey(properties.getType())) {
                    Class[] cArgs = new Class[2];
                    cArgs[0] = JSONObject.class;
                    cArgs[1] = AMQP.BasicProperties.class;
                    try {
                        Method m = s.rpcCallback.getClass().getMethod(alias.get(properties.getType()), cArgs);
                        res = (JSONObject) m.invoke(s.rpcCallback, content, properties);
                    } catch (Exception e) {
                        e.getMessage();
                    }
                } else {
                    res.put("rpc_error", "method not found");
                }
                String reply = String.format("client.%s.%s", properties.getReplyTo(), properties.getAppId());
                AMQP.BasicProperties props = new AMQP.BasicProperties().builder()
                        .appId(s.appId)
                        .replyTo(s.appName)
                        .correlationId(properties.getMessageId())
                        .messageId(Synapse.randomString())
                        .type(properties.getType()).build();
                ch.basicPublish(s.sysName, reply, props, res.toJSONString().getBytes());
                if (s.debug) {
                    Synapse.log(String.format("Rpc Return: (%s)%s@%s->%s %s", properties.getMessageId(), properties.getType(), s.appName, properties.getReplyTo(), res.toJSONString()), Synapse.LOGDEBUG);
                }
            }
        };
        ch.basicConsume(queueName, false, consumer);
    }
}
