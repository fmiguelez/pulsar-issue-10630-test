package es.fmp.pulsar.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public class PartitionedTopicSeekIT {

    private static final String PULSAR_URL = "pulsar://localhost:6650";
    private static final String PULSAR_ADMIN_URL = "http://localhost:8081";
    private static final String TENANT = "public";
    private static final String NAMESPACE = "default";
    private static final String TOPIC = "persistent://" + TENANT + "/" + NAMESPACE + "/test-seek";
    private static final String SUBSCRIPTION_NAME = "seek-test-" + java.util.UUID.randomUUID();
    private static final int PARTITIONS = 4;

    private static final List<String> OBJS = ImmutableList.of("A", "B", "C", "D");

    private static PulsarClient pulsarClient;
    private static PulsarAdmin pulsarAdmin;

    @BeforeAll
    public static void setupPulsarClient() throws IOException {
        pulsarClient = PulsarClient.builder().serviceUrl(PULSAR_URL).build();
        pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(PULSAR_ADMIN_URL).build();
    }

    @BeforeEach
    public void setup() throws Exception {
        List<String> topics = pulsarAdmin.topics().getPartitionedTopicList("public/default");
        if (topics.contains(TOPIC)) {
            pulsarAdmin.topics().deletePartitionedTopic(TOPIC);
        }
        pulsarAdmin.topics().createPartitionedTopic(TOPIC, PARTITIONS);
    }

    @Test
    public void testEarliest() throws Exception {
        testSeek(SubscriptionInitialPosition.Earliest);
    }

    @Test
    public void testLatest() throws Exception {
        testSeek(SubscriptionInitialPosition.Latest);
    }

    private List<String> produce(int suffix) throws PulsarClientException {
        List<String> producedObjs = new ArrayList<>();
        try (Producer<String> prod = pulsarClient.newProducer(Schema.STRING).topic(TOPIC).create()) {
            OBJS.stream().map(s -> s + suffix).peek(producedObjs::add).forEach(prod::sendAsync);
            prod.flush();
        }
        return producedObjs;
    }

    private void testSeek(SubscriptionInitialPosition pos) throws Exception {
        produce(0);
        Thread.sleep(2000);
        long seekTimestamp = System.currentTimeMillis();
        Thread.sleep(2000);
        List<String> secondBatch = produce(1);

        List<String> receivedObjs = new ArrayList<>();

        try (Consumer<String> con = pulsarClient.newConsumer(Schema.STRING).topic(TOPIC).subscriptionName(SUBSCRIPTION_NAME).subscriptionInitialPosition(pos)
                .subscribe()) {
            con.seek(seekTimestamp);

            Message<String> msg = null;
            while ((msg = con.receive(5, TimeUnit.SECONDS)) != null) {
                receivedObjs.add(msg.getValue());
            }
        } catch (Exception e) {
            fail(e);
        }

        /*
         * We should only receive second set of objects
         */
        assertEquals(secondBatch, receivedObjs);
    }

}
