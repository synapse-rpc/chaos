package rpc.synapse.chaos;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.Hashtable;

public class RpcClient {
    private Synapse s;
    private Channel ch;
    private String queueName;
    private String router;
    private Hashtable<String, JSONObject> responseCache;

    RpcClient(Synapse synapse) throws Exception {
        s = synapse;
        ch = synapse.createChannel(0, "RpcClient");
        queueName = String.format("%s_%s_client_%s", synapse.sysName, synapse.appName, synapse.appId);
        router = String.format("client.%s.%s", synapse.appName, synapse.appId);
        responseCache = new Hashtable<>();
    }

    private void checkAndCreateQueue() throws Exception {
        ch.queueDeclare(queueName, true, false, true, null);
        ch.queueBind(queueName, s.sysName, router);
    }

    public void run() throws Exception {
        checkAndCreateQueue();
        Consumer consumer = new DefaultConsumer(ch) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String content = new String(body, "UTF-8");
                responseCache.put(properties.getCorrelationId(), JSON.parseObject(content));
                if (s.debug) {
                    Synapse.log(String.format("RPC Response: (%s)%s@%s->%s %s", properties.getCorrelationId(), properties.getType(), properties.getReplyTo(), s.appName, content), Synapse.LOGDEBUG);
                }
            }
        };
        ch.basicConsume(queueName, true, consumer);
    }

    public JSONObject send(String app, String method, JSONObject param) throws Exception {
        String jsonStr = param.toJSONString();
        String router = String.format("server.%s", app);
        AMQP.BasicProperties props = new AMQP.BasicProperties().builder()
                .appId(s.appId)
                .messageId(Synapse.randomString())
                .type(method)
                .replyTo(s.appName).build();
        ch.basicPublish(s.sysName, router, props, jsonStr.getBytes());
        if (s.debug) {
            Synapse.log(String.format("Rpc Request: (%s)%s->%s@%s %s", props.getMessageId(), s.appName, method, app, jsonStr), Synapse.LOGDEBUG);
        }
        JSONObject ret;
        long ts = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - ts > s.rpcTimeout * 1000) {
                ret = new JSONObject();
                ret.put("rpc_error", "timeout");
                break;
            }
            if (responseCache.containsKey(props.getMessageId())) {
                ret = responseCache.get(props.getMessageId());
                responseCache.remove(props.getMessageId());
                break;
            }
        }
        return ret;
    }
}
