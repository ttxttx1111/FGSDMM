package parallel;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by ttx on 2017/6/15.
 */
public class Dic {
    HashMap<String, Integer> wordCount;
    HashMap<String, Integer> wordToIdMap;

    Dic(String dataDir) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(dataDir));
        String line;
        wordCount = new HashMap<>();
        while((line=in.readLine()) != null){
            JSONObject obj = new JSONObject(line);
            String text = obj.getString("text");
            wordCount(text, wordCount);
        }
        in.close();
    }

    private void wordCount(String text,HashMap<String, Integer> wordCount){
        StringTokenizer st = new StringTokenizer(text);
        String token;

        while(st.hasMoreTokens()){
            token = st.nextToken();
            if (wordCount.containsKey(token)) {
                wordCount.put(token, wordCount.get(token)+1);
            } else {
                wordCount.put(token, 1);
            }
        }

    }

    public void setWordFreThresh(int wordFreThresh){
        ArrayList<String> delete = new ArrayList<>();
        for(Map.Entry<String, Integer> entry:wordCount.entrySet()){
            if(entry.getValue() <= wordFreThresh){
                delete.add(entry.getKey());
            }
        }
        for(String key:delete){
            wordCount.remove(key);
        }
    }

    public void generateWordToIdMap(){
        wordToIdMap = new HashMap<>();
        int count = 0;
        for(Map.Entry<String, Integer> entry:wordCount.entrySet()){
            wordToIdMap.put(entry.getKey(),count++);
        }
    }

}
