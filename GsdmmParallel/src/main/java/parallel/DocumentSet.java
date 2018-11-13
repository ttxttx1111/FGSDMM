package parallel;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class DocumentSet{
	int D = 0;
	ArrayList<Document> documents = new ArrayList<Document>();
	
	public DocumentSet(String dataDir, HashMap<String, Integer> wordToIdMap) 
			 					throws Exception
	{
		BufferedReader in = new BufferedReader(new FileReader(dataDir));
		String line;
		int wordNum = 0;
		int len = 0;

		while((line=in.readLine()) != null){
			D++;
			JSONObject obj = new JSONObject(line);
			String text = obj.getString("text");
			Document document = new Document(text, wordToIdMap);
			documents.add(document);

//			wordNum += document.wordNum;
//			for(int i:document.wordFreArray){
//				len +=i;
//			}
		}
//		System.out.println("average v:" + wordNum/D);
//		System.out.println("average len:" + len/D);
//		System.out.println("V:" + wordToIdMap.size());
		
		in.close();
	}
}
