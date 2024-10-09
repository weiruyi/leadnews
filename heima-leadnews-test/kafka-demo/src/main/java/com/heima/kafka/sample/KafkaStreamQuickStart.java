package com.heima.kafka.sample;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.ValueMapper;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class KafkaStreamQuickStart {
	public static void main(String[] args) {
		//kafka的配置信息
		Properties prop = new Properties();
		prop.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"49.234.52.192:9092");
		prop.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		prop.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
		prop.put(StreamsConfig.APPLICATION_ID_CONFIG,"streams-quickstart");

		//stream构建器
		StreamsBuilder streamsBuilder = new StreamsBuilder();

		//流式计算
		streamProcessor(streamsBuilder);

		//创建kafkaStreams对象
		KafkaStreams kafkaStreams = new KafkaStreams(streamsBuilder.build(), prop);
		//开启流式计算
		kafkaStreams.start();

	}

	/**
	 * 流式计算
	 * @param streamsBuilder
	 */
	private static void streamProcessor(StreamsBuilder streamsBuilder) {
		//创建KStream对象，指定从哪个topic接收消息
		KStream<String, String> stream = streamsBuilder.stream("itcast-topic-input");
		//处理消息的value
		stream.flatMapValues(new ValueMapper<String, Iterable<String>>() {
			@Override
			public Iterable<String> apply(String value) {
				return Arrays.asList(value.split(" "));
			}
		})
				.groupBy((key, value) -> value) //按照value进行聚合
				.windowedBy(TimeWindows.of(Duration.ofSeconds(10))) //时间窗口
				.count() //统计单词个数
				.toStream() //转换为KStream
				.map((key,value)->{
					System.out.println("key:"+key+",vlaue:"+value);
					return new KeyValue<>(key.key().toString(),value.toString());
				})
				//发送消息
				.to("itcast-topic-out");
	}
}
