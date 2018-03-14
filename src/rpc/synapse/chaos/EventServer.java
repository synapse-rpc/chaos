package rpc.synapse.chaos;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;

public class EventServer {
    private Synapse s;
    private Channel ch;
    private String queueName;
    private Hashtable<String, String> alias;

    EventServer(Synapse synapse) throws Exception {
        s = synapse;
        ch = s.createChannel(s.eventProcessNum, "EventServer");
        queueName = String.format("%s_%s_event", s.sysName, s.appName);
        alias = s.eventCallback.regAlias();
    }

    private void checkAndCreateQueue() throws Exception {
        ch.queueDeclare(queueName, true, false, true, null);
        Enumeration keys = alias.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            ch.queueBind(queueName, s.sysName, String.format("event.%s", key));
            Synapse.log(String.format("*EVT %s -> %s", key, alias.get(key)));
        }
    }

    public void run() throws Exception {
        checkAndCreateQueue();
        Consumer consumer = new DefaultConsumer(ch) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                JSONObject content = JSON.parseObject(new String(body, "UTF-8"));
                if (s.debug) {
                    Synapse.log(String.format("Event Receive: %s@%s %s", properties.getType(), properties.getReplyTo(), content.toJSONString()), Synapse.LOGDEBUG);
                }
                String key = String.format("%s.%s", properties.getReplyTo(), properties.getType());
                if (alias.containsKey(key)) {
                    try {
                        Class[] cArgs = new Class[2];
                        cArgs[0] = JSONObject.class;
                        cArgs[1] = AMQP.BasicProperties.class;
                        Method m = s.eventCallback.getClass().getMethod(alias.get(key), cArgs);
                        boolean res = (boolean) m.invoke(s.eventCallback, content, properties);
                        if (res) {
                            ch.basicAck(envelope.getDeliveryTag(), false);
                        } else {
                            ch.basicNack(envelope.getDeliveryTag(), false, true);
                        }
                    } catch (Exception e) {
                        System.out.println(e.toString());
                    }
                } else {
                    ch.basicNack(envelope.getDeliveryTag(), false, false);
                }
            }
        };
        ch.basicConsume(queueName, false, consumer);
    }
}
