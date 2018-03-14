package rpc.synapse.chaos;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

public class EventClient {
    private Synapse s;
    private Channel ch;

    EventClient(Synapse synapse) throws Exception {
        s = synapse;
        ch = synapse.createChannel(0, "EventClient");
    }

    public void send(String event, JSONObject param) throws Exception {
        String jsonStr = param.toJSONString();
        String router = String.format("event.%s.%s", s.appName, event);
        BasicProperties props = new BasicProperties().builder()
                .appId(s.appId)
                .messageId(Synapse.randomString())
                .replyTo(s.appName)
                .type(event).build();
        ch.basicPublish(s.sysName, router, props, jsonStr.getBytes());
        if (s.debug) {
            Synapse.log(String.format("Event Publish: %s@%s %s", event, s.appName, jsonStr), Synapse.LOGDEBUG);
        }
    }


}
