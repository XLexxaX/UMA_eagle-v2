package org.aksw.limes.core.io.query;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aksw.limes.core.io.cache.HybridCache;
import org.aksw.limes.core.io.config.KBInfo;
import org.junit.Test;

public class FileQueryModuleTest {

    @Test
    public void fillCacheTest() {
        HashMap<String, String> prefixes = new HashMap<>();
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixes.put("dbpo", "http://dbpedia.org/ontology/");

        LinkedHashMap<String, Map<String, String>> functions = new LinkedHashMap<>();

        KBInfo kbinfo = new KBInfo(
                "DBpedia",                                                            //String id
//                "resources/ibuprofen.nt",                                            //String endpoint
                Thread.currentThread().getContextClassLoader().getResource("ibuprofen.nt").getPath(),
                null,                                                                //String graph
                "?x",                                                                //String var
                new ArrayList<String>(Arrays.asList("rdfs:label","dbpo:abstract")),                    //List<String> properties
                null,                    //List<String> optionlProperties
                new ArrayList<String>(Arrays.asList("?x rdf:type dbpo:Drug")),        //ArrayList<String> restrictions
                functions,                                                        //LinkedHashMap<String, Map<String, String>> functions
                prefixes,                                                            //Map<String, String> prefixes
                1000,                                                                //int pageSize
                "N3",                                                                //String type
                -1,                                                               //int minOffset
                -1                                                                //int maxoffset
        );
        FileQueryModule fqm = new FileQueryModule(kbinfo);
        HybridCache cache = new HybridCache();
        fqm.fillCache(cache);

        assertTrue(cache.size() > 0);
        for(String abs: cache.getAllInstances().get(0).getProperty("dbpo:abstract")){
        	if(abs.contains("@en")){
        		System.out.println(abs);
        	}
        }
    }

}
