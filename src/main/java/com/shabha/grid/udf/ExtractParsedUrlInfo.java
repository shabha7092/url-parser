package com.shabha.grid.udf;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileNotFoundException;
import java.util.stream.Collectors;
import java.util.*;
import edu.stanford.nlp.wordseg.CorpusDictionary;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.process.Morphology;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;



public class ExtractParsedUrlInfo {

    private static final String FILE_NAME = "words.txt";
    private static Pattern pattern = Pattern.compile("[^A-Za-z0-9]");
    private static Set<String> dictionary = null;
    private static StanfordCoreNLP pipeline = null;
    private static MaxentTagger tagger = null;
    private static Morphology morphology = null;

    public StanfordCoreNLP getPipeline() {
        if(pipeline == null) {
            Properties properties = new Properties();
            properties.setProperty("annotators", "tokenize,ssplit"); 
            pipeline = new StanfordCoreNLP(properties);
        }
        return pipeline;
    }

    public MaxentTagger getTagger() {
        if(tagger == null) {
            tagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");
        }
        return tagger;
    }

    public Morphology getMorphology() {
        if(morphology == null) {
            morphology = new Morphology();
        }
        return morphology;
    }

    public Set<String> getDictionary() {
        if(dictionary == null) {
            CorpusDictionary corpus = new CorpusDictionary(FILE_NAME);
            dictionary = corpus.getTable();
            dictionary = dictionary.stream().map(String::toLowerCase).collect(Collectors.toSet());
        }
        return dictionary;
    }


    public List<String> readData(String fileName) throws FileNotFoundException, UnsupportedEncodingException {
        if(fileName == null || fileName.length() == 0) {
            return null;
        }
        List<String> urls = new ArrayList<String>();
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()) {
            String[] split  = scanner.nextLine().toString().split("\t");
            if(split[split.length-1].length() > 2) {
                String domain_path = split[split.length-2];
                String query_string = split[split.length-1].replace('[', ' ').replace(']', ' ');
                if (query_string.length() > 0) {
                    Map<String, String> query_params = Arrays.stream(query_string.split(","))
                            .map(s -> s.split("#"))
                            .collect(Collectors.toMap(a -> a[0], a -> a[1]));
                    System.out.println(extractQueryParamsInfo(query_params));
                }   
            } 
        }
        scanner.close();
        return urls;
    }

    public String extractQueryParamsInfo(Map<String, String> map) throws UnsupportedEncodingException  {
        if(map == null || map.size() == 0) {
            return "";
        }
        List<String> list = new ArrayList<String>();

        for (Map.Entry<String, String> entry : map.entrySet() ) {
            String value = entry.getValue();
            if(value != null && value.length() != 0) {
                /* custom enocoded data by python */
                if(value.contains("$sep")) {
                    value = value.replaceAll("$sep", ",");
                } else if (value.contains("%sep")) {
                    value = value.replaceAll("%sep", ",");
                }
                /* Stanford NLP Parser Reg ex's */
                value = value.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
                value = value.replaceAll("\\+", "%2B");
                value = URLDecoder.decode(value, "UTF-8").trim();
                if(value.length() != 0) {
                    CoreDocument document = new CoreDocument(value);
                    getPipeline().annotate(document);
                    List<CoreLabel> tokens = document.sentences().get(0).tokens();
                    for (CoreLabel token : tokens) {
                        String word = token.word().toLowerCase();
                        Matcher matcher = pattern.matcher(word);
                        if(matcher.find()) {
                            String text = word.replaceAll("[^A-Za-z0-9]", " ");
                            String[] words = text.split(" ");
                            if(isValidWordArray(words)) {
                                if(!list.contains(word)) {
                                    list.add(word);
                                }
                            }
                        } else if (isValidWord(word)) {
                            if(!list.contains(word)) {
                                list.add(word);
                            }
                        }
                    } 
                }
            }
        }
        return buildQueryParamsString(list);
    }

    public boolean isValidWord(String word) {
        if(word == null || word.length() == 0) {
            return false;
        }
        if(getDictionary().contains(word)) {
            return true;
        } else if (getDictionary().contains(getMorphology().stem(word))) {
            return true;
        } 
        String tag = getTagger().tagString(word);
        if(getDictionary().contains(getMorphology().lemma(word, tag))) {
            return true;
        }
        return false;
    }

    public boolean isValidWordArray(String[] words) {
        if(words == null || words.length == 0) {
            return false;
        }
        for(String word : words) {
            if(!isValidWord(word)) {
                return false;
            }   
        }
        return true;
    }

    public String buildQueryParamsString(List<String> wordList) {
        if(wordList == null || wordList.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for(String word : wordList) {
            sb.append(word + "_");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
