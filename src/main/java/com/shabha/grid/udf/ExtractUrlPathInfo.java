package com.shabha.grid.udf;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.*;
import edu.stanford.nlp.wordseg.CorpusDictionary;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.process.Morphology;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.pig.EvalFunc;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;



public class ExtractUrlPathInfo extends EvalFunc<String> {

    private static final String FILE_NAME = "python_corpus.txt";
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


    @Override
    public String exec(Tuple input) throws IOException {
        try {
            if (input == null || input.size() < 1) {
                throw new IOException("Not enough arguments to " + this.getClass().getName() + ": got " + input.size() + ", expected at least 1");
            }
            if (input.get(0) == null) {
                return "";
            }
            String value = (String) input.get(0);
            return extractUrlPathInfo(value);
        } catch (ExecException e) {
            throw new IOException(e);
        }
    }

  
    public String extractUrlPathInfo(String value) throws UnsupportedEncodingException  {
        if(value == null || value.length() == 0) {
            return "";
        }
        List<String> list = new ArrayList<String>();
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
        CoreDocument document = new CoreDocument(value);
        getPipeline().annotate(document);
        List<CoreLabel> tokens = document.sentences().get(0).tokens();
        for (CoreLabel token : tokens) {
            String word = token.word().toLowerCase();
            Matcher matcher = pattern.matcher(word);
            if(matcher.find()) {
                String text = word.replaceAll("[^A-Za-z0-9]", " ");
                String[] words = text.split(" ");
                for (String s : words) { 
                    if(isValidWord(s)) {
                        if(!list.contains(s)) {
                            list.add(s);
                        }
                    }
                }
            } else if (isValidWord(word)) {
                if(!list.contains(word)) {
                    list.add(word);
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

    @Override
    public Schema outputSchema(Schema input) {
        return new Schema(new Schema.FieldSchema(null, DataType.CHARARRAY));
    }

    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        // TODO Auto-generated method stub
        ExtractUrlPathInfo extract = new ExtractUrlPathInfo();
        System.out.println(extract.extractUrlPathInfo("ret=html&limit=8&btp=1&phint=eid%3D283&phint=tcat%3D617&phint=bin%3D0.0&phint=iid%3D253848729598&phint=type%3Duser&phint=pid%3D&phint=meta%3D11232&phint=cg%3D9589b87c1610a9c46f17bb98fa92ad41&phint=item%3DBluray+Movie+Lot+%2853+Titles%29+-+Great+Movies+Great+Condition+the+Hangover%2C+Snitch&phint=lx%3D0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|0|cd|0|&phint=ps%3D&phint=uid%3D905181343&phint=encuser%3D43952774642&phint=fm_segment%3D2&phint=list2%3D3&phint=pageId%3D2047675&phint=MSD%3D0&phint=rlsa_seg%3D1,1&phint=user_type%3D22,33&phint=split%3D2&phint=lp%3D0,617.0,302877704293,&phint=lb%3D,,302877704293,&phint=userlogin%3D22"));
        
    }

}
