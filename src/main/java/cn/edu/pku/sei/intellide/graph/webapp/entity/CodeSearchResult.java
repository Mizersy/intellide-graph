package cn.edu.pku.sei.intellide.graph.webapp.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.neo4j.graphdb.GraphDatabaseService;

import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.entity.NLPToken;
import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.entity.TokenMapping.NLPMapping;

public class CodeSearchResult{
	public Neo4jSubGraph neo4jSubGraph;
	public String answerNodes;
	public List<Ambiguity> ambiguities = new ArrayList<Ambiguity>();
	
	public CodeSearchResult() {}
	
	public CodeSearchResult(List<Long> nodeIds, List<Long> relIds, GraphDatabaseService db) {
		neo4jSubGraph = new Neo4jSubGraph(nodeIds, relIds, db);
	}
	
	public CodeSearchResult(Neo4jSubGraph neo4jSubGraph,String answersNodes) {
		this.answerNodes = answersNodes;
		this.neo4jSubGraph = neo4jSubGraph;
	}
	
	public void addAmbiguity(NLPToken token,String cypher) {
		Ambiguity ambiguty = new Ambiguity(token.text);
		Map<String, Integer> map = new HashedMap<String,Integer>();
		for (NLPMapping mapping : token.mappingList) {
			if (!map.containsKey(mapping.mapLabels)) {
				ambiguty.addMap(mapping.mapValue, mapping.mapType, mapping.mapLabels);
				map.put(mapping.mapLabels, 1);
			}
				
		}
		ambiguty.cypher = cypher;
		ambiguities.add(ambiguty);
	}
}