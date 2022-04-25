package cn.edu.pku.sei.intellide.graph.qa.nl_query;

import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.NLPInterpreter;
import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.ir.LuceneIndex;
import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.entity.NLPNode;
import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.entity.NLPToken;
import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.entity.Query;
import cn.edu.pku.sei.intellide.graph.webapp.entity.CodeSearchResult;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jNode;
import cn.edu.pku.sei.intellide.graph.webapp.entity.Neo4jSubGraph;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.kernel.impl.core.NodeProxy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public class NLQueryEngine {

    public static String languageIdentifier;
    private static GraphDatabaseService db;
    private static String dataDirPath;
    private static final String[] englishQuestionIndicators = new String[]{"who", "what", "which", "when", "list", "show"};
    public static Query query_static;
    public static List<Query> answers;

    public NLQueryEngine(GraphDatabaseService db, String dataDirPath, String languageIdentifier) {
        this.db = db;
        this.dataDirPath = dataDirPath;
        this.languageIdentifier = languageIdentifier;
        createIndex();
    }

    private static boolean isNlpSolver(String query) {
        for (String indicator : englishQuestionIndicators){
            if (query.trim().toLowerCase().startsWith(indicator+" ")){
                return true;
            }
        }
        return query.matches("\\d+");
    }

    public CodeSearchResult search(String queryString,Boolean isChat,String mapValue) {
    	log.info("get in in");

        // Hack: 对于英文问句，只解析具有特定模式的句子，否则不做解析，交给下一个模块处理.
        /*
        if (languageIdentifier.equals("english") && !isNlpSolver(queryString)) {
            return new CodeSearchResult();
        }
        */
        log.info("get in");
        List<Long> nodes = new ArrayList<>();
        List<Long> rels = new ArrayList<>();
        List<Long> retNodes = new ArrayList<>();
        
        String cypherret;
        String answerNodes = "";

        if (queryString.matches("\\d+")) {
            //输入数字，则返回ID为该数字的结点
            String c = "Match (n) where id(n)=" + queryString + " return n, id(n), labels(n)";
            Result p = db.execute(c + " limit 30");
            while (p.hasNext()) {
                Map m = p.next();
                nodes.add((Long) m.get("id(n)"));
            }
            cypherret = c;
        } else {
        	if (!isChat) {
        		answers = NLPInterpreter.createInstance(db, languageIdentifier).pipeline(queryString);
        		log.debug("answers get!!!! size==" + String.valueOf(answers.size()));
        		if (answers != null && answers.size() > 0)
	        		for(Query cypherTmp : answers){
	                    log.debug("问句语义解析结果：" + cypherTmp.cypher);
	                    //cypherTmp.printTokenMapping();
	                    cypherTmp.alive = true;
	                }
        	}else {
        		for (Query qTmp : answers) {
        			for (NLPToken token : qTmp.tokens) {
        				//log.info(token.text);
        				//log.info(token.mapping.mapLabels);
        				if(token.text.equals(queryString) && (token.mapping != null && !token.mapping.mapLabels.equals(mapValue))) {
        					qTmp.alive = false;
        				}
        			}
        		}
        	}
            log.debug("answers get!");
            if (answers == null || answers.size() == 0) return new CodeSearchResult(nodes, rels, db);
            /*
            for(Query cypherTmp : answers){
                //log.debug("问句语义解析结果：" + cypherTmp.cypher);
                //cypherTmp.printTokenMapping();
                cypherTmp.alive = true;
            }
            */
            
            String c = answers.get(0).cypher;
            for (int i = 0;i < answers.size();++i) {
            	log.debug(String.valueOf(answers.get(i).alive)+"问句语义解析结果：" + answers.get(i).cypher);
            	if (answers.get(i).alive) {
            	//if (answers.get(i).alive && answers.get(i).cypher.contains("n1:Method") && !answers.get(i).cypher.contains("Method)<-[:methodCall]")) {
            		c = answers.get(i).cypher;
            		break;
            	}
            }
            log.debug("问句语义解析结果：" + c);
            /*
            for (NLPNode node : answers.get(0).nodes) {
            	log.debug("node.id : " + node.id);
            }
            */
            String returnT;
            String whereT;
            String matchT;
            if (!c.contains("WHERE")) {
                returnT = c.substring(c.indexOf("RETURN") + 7, c.length());
                whereT = "WHERE (true)";
                matchT = c.substring(c.indexOf("MATCH"), c.indexOf("RETURN"));
            } else {
                returnT = c.substring(c.indexOf("RETURN") + 7, c.length());
                matchT = c.substring(c.indexOf("MATCH"), c.indexOf("WHERE"));
                whereT = c.substring(c.indexOf("WHERE"), c.indexOf("RETURN"));
            }
            String nodeid;
            if (!returnT.contains("labels")) {
                nodeid = returnT.substring(0, returnT.indexOf("."));
                //???
                //c = c.substring(0, c.indexOf("RETURN") + 7) + String.format("%s,id(%s),labels(%s)", nodeid);
                c = c.substring(0, c.indexOf("RETURN") + 7) + String.format("%s,id(%s),labels(%s)", nodeid, nodeid, nodeid);
            } else nodeid = returnT.substring(0, returnT.indexOf(","));

            Result p = db.execute(c.replaceAll("RETURN", "RETURN distinct") + " limit 10");
            cypherret = c;
            while (p.hasNext()) {
                Map m = p.next();
                retNodes.add((Long) m.get("id(" + nodeid + ")"));
            }
            
            for (Long id : retNodes) {
                //log.debug("retNodes.id: "+id);
                answerNodes += Neo4jNode.get(id, db).getProperties().get("name") + " ";
            	String tmpc = "MATCH p= " + matchT.substring(5, matchT.length());
                tmpc += whereT + " AND (id(" + nodeid + ")=" + id + ")";
                tmpc += "return p";
                Result pr = db.execute(tmpc + " limit 1");
                while (pr.hasNext()) {
                    Map m = pr.next();
                    Path obj = (Path) m.get("p");
                    Iterator iter = obj.nodes().iterator();
                    while (iter.hasNext()) {
                        NodeProxy nodep = (NodeProxy) iter.next();
                        nodes.add(nodep.getId());
                    }
                    iter = obj.relationships().iterator();
                    while (iter.hasNext()) {
                        Relationship relp = (Relationship) iter.next();
                        rels.add(relp.getId());
                    }
                }
            }
            log.debug("answer1:"+answerNodes);
        }
        Neo4jSubGraph ret = new Neo4jSubGraph(nodes, rels, db);
        ret.setCypher(cypherret);
        if (!cypherret.toLowerCase().contains("where")) {
            ret.getNodes().clear();
            ret.getRelationships().clear();
            ret.setCypher("");
        }
        CodeSearchResult codeSearchResult = new CodeSearchResult(ret,answerNodes);
        for (NLPToken token : query_static.tokens) {
            if (token.mappingList.size() > 1)
        	    codeSearchResult.addAmbiguity(token,cypherret);
        }
        log.debug("answer2:"+codeSearchResult.answerNodes);
        return codeSearchResult;
    }

    private void createIndex() {
        if (new File(dataDirPath + "/index").exists())
            return;
        LuceneIndex LI = LuceneIndex.createInstance(db, dataDirPath);
        try {
            LI.index();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
