/**************************************************************************************
 * Copyright (C) 2008 EsperTech, Inc. All rights reserved.                            *
 * http://esper.codehaus.org                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.linear.kafka.output;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

import org.linear.kafka.input.InputEventInjectorClient;
import org.linear.kafka.util.Constants;

public class Output
{
    private static Log log = LogFactory.getLog(Output.class);
    private ExecutorService executor;
    private final ConsumerConnector consumer;
    private final String in_comming_topic = "output_topic";
    
    private final int numThreads;
    private static boolean shutdownFlag = false;;
    
    public Output(){
        Properties props = new Properties();       
        InputStream propertiesIS = Output.class.getClassLoader().getResourceAsStream(org.linear.kafka.util.Constants.CONFIG_FILENAME_OUTPUT);
        if(propertiesIS == null){
        	throw new RuntimeException("Properties file '" + org.linear.kafka.util.Constants.CONFIG_FILENAME_OUTPUT + "' not found in the classpath");
        }
        try {
			props.load(propertiesIS);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        numThreads = Integer.parseInt(props.getProperty(org.linear.kafka.util.Constants.NUM_THREADS));
    	consumer = Consumer.createJavaConsumerConnector(new ConsumerConfig(props));
    }
        
    public void run(){
    	Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
    	topicCountMap.put(in_comming_topic, new Integer(numThreads));
    	Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
    	List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(in_comming_topic);
    	   	
    	//Next, we launch all the threads
    	executor = Executors.newFixedThreadPool(numThreads);
    	
    	//Next we create an object to consume the message
    	int threadNumber = 0;
    	
    	for(final KafkaStream stream : streams){
    		executor.submit(new OutputConsumer(stream, threadNumber));
    		threadNumber++;
    	}
    }

    public void shutdown(){
    	if (consumer != null){
    		consumer.shutdown();
    	}
    	
    	if(executor != null){
    		executor.shutdown();
    	}
    }
    
    public static void main(String[] args) throws Exception
    {    	
    	Output output = new Output();
    	output.run();
    	
    	//Just wait infinitely.
    	while(!shutdownFlag){
    		try{
    			Thread.sleep(1000);
    		}catch(InterruptedException ie){
    			//Just do nothing
    		}
    	}
    	
    	output.shutdown();
    }
}
