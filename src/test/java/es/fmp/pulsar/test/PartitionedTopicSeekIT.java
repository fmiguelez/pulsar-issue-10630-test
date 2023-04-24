package es.fmp.pulsar.test;

import com.google.common.collect.ImmutableList;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Reader;
import org.apache.pulsar.client.api.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

  private MessageId lastPublishedMessageId;

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
  public void testSeekTimestampFromEarliest() throws Exception {
    testSeek(MessageId.earliest);
  }

  @Test
  public void testSeekTimestampFromLatest() throws Exception {
    testSeek(MessageId.latest);
  }

  @Test
  public void testSeekMessageIdFromLatest() throws Exception {
    testSeek(null);
  }

  private List<String> produce(int suffix) throws PulsarClientException, ExecutionException, InterruptedException {
    List<String> producedObjs = new ArrayList<>();
    try (Producer<String> prod = pulsarClient.newProducer(Schema.STRING).topic(TOPIC).create()) {
      CompletableFuture<MessageId> lastMessageId = null;
      for (String s : OBJS) {
        String value = s + suffix;
        producedObjs.add(value);
        lastMessageId = prod.newMessage().key(s).value(value).sendAsync();
      }
      prod.flush();
      if(lastMessageId != null) {
        lastPublishedMessageId = lastMessageId.get();
      }
    }

    return producedObjs;
  }

  private void testSeek(MessageId pos) throws Exception {
    produce(0);
    MessageId firstBatchLastMessageId = lastPublishedMessageId;
    Thread.sleep(2000);
    long seekTimestamp = System.currentTimeMillis();
    Thread.sleep(2000);
    List<String> secondBatch = produce(1);

    List<String> receivedObjs = new ArrayList<>();

    MessageId startMessageId = pos == null ? MessageId.latest : pos;

    try (Reader<String> con = pulsarClient
        .newReader(Schema.STRING)
        .topic(TOPIC)
        .subscriptionName(SUBSCRIPTION_NAME)
        .startMessageId(startMessageId)
        .create()) {
      if (pos != null) {
        con.seek(seekTimestamp);
      } else {
        con.seek(firstBatchLastMessageId);
      }

      Message<String> msg;
      while ((msg = con.readNext(5, TimeUnit.SECONDS)) != null) {
        receivedObjs.add(msg.getValue());
      }
    }

    /*
     * We should only receive second set of objects
     */
    assertEquals(secondBatch.size(), receivedObjs.size(), "size");
    assertThat(receivedObjs, containsInAnyOrder(secondBatch.toArray()));
  }

}
