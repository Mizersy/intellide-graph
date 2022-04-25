package cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.extractmodel;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class GraphSchemaKeywords {
    public static GraphSchemaKeywords single = null;
    public Map<String, Pair<String, String>> types = new HashMap<>();

    private GraphSchemaKeywords() {
        types.put("Class", new ImmutablePair<>("name", "fullName"));
        types.put("Method", new ImmutablePair<>("name", "fullName"));
        types.put("Field", new ImmutablePair<>("name", "fullName"));
        types.put("Docx", new ImmutablePair<>("title", "title"));
        types.put("StackOverflowQuestion", new ImmutablePair<>("title", "title"));
        types.put("StackOverflowComment", new ImmutablePair<>("commentId", "commentId"));
        types.put("StackOverflowUser", new ImmutablePair<>("displayName", "displayName"));
        types.put("StackOverflowAnswer", new ImmutablePair<>("answerId", "answerId"));
        types.put("GitUser", new ImmutablePair<>("name", "name"));
        types.put("JiraIssue", new ImmutablePair<>("name", "name"));
        types.put("JiraIssueComment", new ImmutablePair<>("id", "id"));
        types.put("JiraIssueUser", new ImmutablePair<>("displayName", "displayName"));
        types.put("Mail", new ImmutablePair<>("mailId", "mailId"));
        types.put("MailUser", new ImmutablePair<>("names", "names"));
        types.put("Commit", new ImmutablePair<>("name", "name"));

        //what's pair for?
        types.put("c_field",new ImmutablePair<>("name", "name"));
        types.put("c_code_file",new ImmutablePair<>("tailFileName", "fileName"));
        types.put("Markdown",new ImmutablePair<>("title", "title"));
        types.put("MarkdownCatalog", new ImmutablePair<String, String>("title", "title") );
        types.put("MarkdownSection", new ImmutablePair<String, String>("title", "title"));
        types.put("c_function", new ImmutablePair<String, String>("name", "fullName"));
        types.put("c_struct", new ImmutablePair<String, String>("name", "name"));
        types.put("c_variable", new ImmutablePair<String, String>("name", "name"));
        
    }

    public static GraphSchemaKeywords getSingle() {
        if (single == null) {
            single = new GraphSchemaKeywords();
        }
        return single;
    }
}
