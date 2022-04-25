package cn.edu.pku.sei.intellide.graph.webapp;

import cn.edu.pku.sei.intellide.graph.qa.code_search.CodeSearch;
import cn.edu.pku.sei.intellide.graph.qa.doc_search.DocSearch;
import cn.edu.pku.sei.intellide.graph.qa.nl_query.NLQueryEngine;
import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.entity.NLPToken;
import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.entity.TokenMapping.NLPMapping;
import cn.edu.pku.sei.intellide.graph.webapp.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.json.JSONException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.edu.pku.sei.intellide.graph.webapp.entity.SnowGraphProject.getProjectList;

@CrossOrigin
@RestController
@Slf4j
public class Controller {

    Map<String, CodeSearch> codeSearchMap = new LinkedHashMap<>();
    Map<String, DocSearch> docSearchMap = new LinkedHashMap<>();
    Map<String, NavResult> navResultMap = new LinkedHashMap<>();
    Map<String, GraphDatabaseService> dbMap = new LinkedHashMap<>();
    CodeSearch codeSearch = null;
    @Autowired
    private Context context;
    //Ambiguity ambiguity;

    @RequestMapping(value = "/projects", method = {RequestMethod.GET, RequestMethod.POST})
    synchronized public List<SnowGraphProject> getProjects() throws IOException, JSONException {
        List<SnowGraphProject> projects;
        projects = getProjectList(context.infoDir);
        return projects;
    }


    @RequestMapping(value = "/codeSearch", method = {RequestMethod.GET, RequestMethod.POST})
    synchronized public CodeSearchResult codeSearch(@RequestParam("query") String query,@RequestParam("project") String project,@RequestParam("test") String test) {

        log.info("==================================================");
        log.info("启动代码搜索(first)，query: " + query);
        log.info("project: "+project);
        log.info("test: "+test);

        String languageIdentifier = "english";
        if (project.contains("chinese")) {
            languageIdentifier = "chinese";
        }

        NLQueryEngine nlQueryEngine = new NLQueryEngine(getDb(project), context.dataDir + '/' + project, languageIdentifier);
        CodeSearchResult codeSearchResult = nlQueryEngine.search(query,false,"");
        for (Ambiguity ambiguity : codeSearchResult.ambiguities) {
        	log.info("Ambiguity of " + ambiguity.text + " :");
        	for (int i = 0;i < ambiguity.mapLabels.size();++i) {
        		log.info("mapValue: " + ambiguity.mapValues.get(i) + " mapType: " + ambiguity.mapTypes.get(i) + " mapLabel: " + ambiguity.mapLabels.get(i));
        	}
        }
        Neo4jSubGraph r = codeSearchResult.neo4jSubGraph;

        //if (r.getNodes().size() > 0) {
            return codeSearchResult;
        //}
        /*
        log.info(String.valueOf(r.getNodes().size()));
        if (!codeSearchMap.containsKey(project)) {
            codeSearchMap.put(project, new CodeSearch(getDb(project), languageIdentifier));
        }

        CodeSearch codeSearch = codeSearchMap.get(project);
        
        return new CodeSearchResult(codeSearch.search(query));
        */
    }

    @RequestMapping(value = "/codeSearchNext", method = {RequestMethod.GET, RequestMethod.POST})
    synchronized public CodeSearchResult codeSearchNext(String query,String project,String ambiguity) {
        log.info("==================================================");
        log.info("answer's size: " + NLQueryEngine.answers.size());
        log.info("tokens.size(): " + NLQueryEngine.query_static.tokens.size());
        /*
        for (NLPToken token : NLQueryEngine.query_static.tokens){
            log.info("token.text : [" + token.text + "] token.maplist.size: " + token.mappingList.size());
            for (NLPMapping mapping : token.mappingList) {
            	log.debug("printTokenMapping "+token.text + " mapValue:" + mapping.mapValue + " mapType: " + mapping.mapType + " mapVlbel: " + mapping.mapLabels);
            }
        }
        */
        log.info("启动代码搜索(NEXT)，query: " + query);
        log.info("project: "+project);
        log.info("ambiguity: "+ambiguity);
        

        String languageIdentifier = "english";
        if (project.contains("chinese")) {
            languageIdentifier = "chinese";
        }
        
        NLQueryEngine nlQueryEngine = new NLQueryEngine(getDb(project), context.dataDir + '/' + project, languageIdentifier);
        log.info(nlQueryEngine.languageIdentifier);
        CodeSearchResult codeSearchResult = nlQueryEngine.search(query,true,ambiguity);
        if(codeSearchResult == null) log.info("error!!!"); 
        if (codeSearchResult.neo4jSubGraph == null) log.info("neo error");
        if (codeSearchResult.neo4jSubGraph.getNodes() == null) log.info("node error");
        log.info(String.valueOf((codeSearchResult.neo4jSubGraph.getNodes().size())));
        Neo4jSubGraph r = codeSearchResult.neo4jSubGraph;
        log.info("get out");
        
        //if (r.getNodes().size() > 0) {
            return codeSearchResult;
        //}
        /*
        if (!codeSearchMap.containsKey(project)) {
            codeSearchMap.put(project, new CodeSearch(getDb(project), languageIdentifier));
        }

        CodeSearch codeSearch = codeSearchMap.get(project);
        codeSearchResult.neo4jSubGraph = codeSearch.search(query);
        return codeSearchResult;
        */
    }

    @RequestMapping(value = "/docSearch", method = {RequestMethod.GET, RequestMethod.POST})
    synchronized public List<Neo4jNode> docSearch(String query, String project) throws IOException, ParseException {

        String languageIdentifier = "english";
        if (project.contains("chinese")) {
            languageIdentifier = "chinese";
        }

        if (!docSearchMap.containsKey(project)) {
            codeSearchMap.put(project, new CodeSearch(getDb(project), languageIdentifier));
            codeSearch = codeSearchMap.get(project);
            docSearchMap.put(project, new DocSearch(getDb(project), context.dataDir + '/' + project + "/doc_search_index", codeSearch));
        }
        DocSearch docSearch = docSearchMap.get(project);
        List<Neo4jNode> retList = docSearch.search(query, project);
        retList.addAll(docSearch.search(query, project, true));
        return retList;
    }

    @RequestMapping(value = "/nav", method = {RequestMethod.GET, RequestMethod.POST})
    synchronized public NavResult nav(String project) {
        if (!navResultMap.containsKey(project)) {
            navResultMap.put(project, NavResult.fetch(getDb(project)));
        }
        NavResult navResult = navResultMap.get(project);
        return navResult;
    }

    @RequestMapping(value = "/relationList", method = {RequestMethod.GET, RequestMethod.POST})
    synchronized public List<Neo4jRelation> relationList(long id, String project) {
        return Neo4jRelation.getNeo4jRelationList(id, getDb(project));
    }

    @RequestMapping(value = "/node", method = {RequestMethod.GET, RequestMethod.POST})
    synchronized public Neo4jNode node(long id, String project) {
        return Neo4jNode.get(id, getDb(project));
    }

    private GraphDatabaseService getDb(String project) {
        if (!dbMap.containsKey(project)) {
            GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(context.graphDir + '/' + project));
            dbMap.put(project, db);
        }
        return dbMap.get(project);
    }

}

@Component
class Context {
    String graphDir = null;
    String dataDir = null;
    String infoDir = null;

    @Autowired
    public Context(Conf conf) {
        this.graphDir = conf.getGraphDir();
        this.dataDir = conf.getDataDir();
        this.infoDir = conf.getInfoDir();
    }

}