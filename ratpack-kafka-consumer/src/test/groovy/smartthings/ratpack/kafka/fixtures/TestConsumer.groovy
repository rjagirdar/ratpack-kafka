package smartthings.ratpack.kafka.fixtures

import com.google.inject.Inject
import groovy.util.logging.Slf4j
import org.apache.kafka.clients.consumer.ConsumerRecords
import smartthings.ratpack.kafka.Consumer
import smartthings.ratpack.kafka.event.KafkaConsumerEvent
import smartthings.ratpack.kafka.event.KafkaConsumerEventListener

@Slf4j
class TestConsumer implements Consumer<byte[], byte[]> {

	final String group
	final String[] topics

	TestService testService
	List<KafkaConsumerEventListener> listeners = new ArrayList<>();

	@Inject
	TestConsumer(TestService testService, String group, String topic) {
		this.testService = testService
		this.group = group
		this.topics = [ topic ]
	}

	@Override
	void consume(ConsumerRecords<byte[], byte[]> records) throws Exception {
		log.debug("Consuming records. [size: ${records.size()}]")
		records.each { record ->
			ByteArrayInputStream bais
			ObjectInput oi
			try {
				bais = new ByteArrayInputStream(record.value());
				oi = new ObjectInputStream(bais);
				TestData data = (TestData) oi.readObject()
				log.debug("Consuming a record. [data: ${data.toString()}]")
				testService.run(data)
			} catch (IOException e) {
				throw e
			} finally {
				if (oi) {
					oi.close()
				}
				if (bais) {
					bais.close()
				}
			}


		}
	}

	@Override
	Long getPollWaitTime() {
		return 3000
	}

	@Override
	void eventHandler(KafkaConsumerEvent event) {
		listeners*.eventNotification(event)
	}

	void addEventListener(KafkaConsumerEventListener listener) {
		listeners.add(listener)
	}
}
