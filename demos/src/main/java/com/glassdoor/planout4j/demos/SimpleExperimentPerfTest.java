package com.glassdoor.planout4j.demos;

import com.glassdoor.planout4j.Namespace;
import com.glassdoor.planout4j.NamespaceConfig;
import com.glassdoor.planout4j.compiler.PlanoutDSLCompiler;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import java.util.Collections;
import java.util.Map;
import java.util.PriorityQueue;

public class SimpleExperimentPerfTest {

    public static void main(String[] args) throws Exception {
        /*String nsName = "demo_namespace";
        String confdir = "demos/conf/";
        FileReader file = new FileReader(Paths.get(confdir, nsName + ".yaml").toFile());
        NamespaceConfig namespaceConfig = new YAMLConfigParser().parseAndValidate(file, nsName);
        */
        Logger.getLogger("com.glassdoor").setLevel(Level.INFO);

        String nsName = "demo_namespace";
        int totalSegments = 10000;
        String unit = "userid";
        String salt = null;
        NamespaceConfig nsConf = new NamespaceConfig(nsName, totalSegments, unit, salt);

        String experimentName = "defaultExperiment";
        String script = "itemsToShow = uniformChoice(choices=[5, 10, 20], unit=userid);";

        nsConf.defineExperiment(experimentName, PlanoutDSLCompiler.dsl_to_json(script));
        nsConf.setDefaultExperiment(experimentName);

        int iterations = 1000000;
        int itemsToShow;
        Map<String, Integer> input;
        Namespace namespace;
        final int worst05Cnt = Math.round(iterations * 0.05f);
        final PriorityQueue<Long> worst05Heap = new PriorityQueue<>(worst05Cnt);
        long start=0, iterTime=0, totalTime=0, minTime = Long.MAX_VALUE, maxTime = 0;

        for (int i = 0; i < iterations; i++) {
            start = System.nanoTime();
            input = Collections.singletonMap(unit, i);//userId
            namespace = new Namespace(nsConf, input, null);
            itemsToShow = namespace.getParam("itemsToShow", 10);
            iterTime = System.nanoTime() - start;
            totalTime += iterTime;
            minTime = Math.min(minTime, iterTime);
            maxTime = Math.max(maxTime, iterTime);
            Long worstHead = worst05Heap.peek();
            if (worst05Heap.size() < worst05Cnt || worstHead < iterTime) {
                if (worst05Heap.size() == worst05Cnt) {
                    worst05Heap.poll();
                }
                worst05Heap.add(iterTime);
            }
        }
        System.out.format("\nPerformed %d iterations in %d millis; min/max/avg/95pct: %d/%d/%d/%d micros\n",
                iterations, totalTime / 1000000, minTime/1000, maxTime/1000,
                (long)(0.001 * totalTime / iterations), worst05Heap.peek()/1000);

    }
}
