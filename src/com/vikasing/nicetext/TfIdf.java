/**
 * 
 */
package com.vikasing.nicetext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jblas.DoubleMatrix;

/**
 * @author vikasing
 *
 */
public class TfIdf {
	private static final String DATA_DIR = "data";
	
	void calculateWordRarity() throws IOException{
		File file = new File(DATA_DIR);
		File[] files = file.listFiles();
		Map<String, Map<String, SortedSet<Entry<String, Integer>>>> ngramDocMap = new HashMap<String, Map<String,SortedSet<Entry<String,Integer>>>>();
		NGramExtracter nGramExtracter = new NGramExtracter();
		StringBuffer allText = new StringBuffer();
		int totalDocs = files.length;

		//Map<String, Integer> sizeMap = new HashMap<String, Integer>();
		for (int i = 0; i < totalDocs; i++) {
			FileInputStream fileStream = null;
			InputStreamReader iReader = null;
			BufferedReader in = null;
			try {
				fileStream = new FileInputStream(files[i]);
				iReader = new InputStreamReader(fileStream);
				in = new BufferedReader(iReader);
				String xString = null;
				StringBuffer stringBuffer = new StringBuffer();
				while ((xString=in.readLine())!=null) {
					stringBuffer.append(xString+" ");
				}
				String text = stringBuffer.toString();
				allText.append(text+" ");
				//int tSize = text.split(" ").length;
				//sizeMap.put(files[i].getName(), tSize);
				ngramDocMap.put(files[i].getName(),nGramExtracter.getNGrams(text));
				
			} catch(Exception e) {
				e.printStackTrace();
			}
			finally {
				in.close();iReader.close();fileStream.close();
			}
		}
		Map<String, SortedSet<Entry<String, Integer>>> allTextNgMap = nGramExtracter.getNGrams(allText.toString());
		SortedSet<Entry<String, Integer>> allTextMonos = allTextNgMap.get("mono");
		SortedSet<Entry<String, Integer>> allTextBis = allTextNgMap.get("bi");
		SortedSet<Entry<String, Integer>> allTextTris = allTextNgMap.get("tri");
		
		SortedSet<String> allMonos = new TreeSet<String>();
		SortedSet<String> allBis = new TreeSet<String>();
		SortedSet<String> allTris = new TreeSet<String>();
				
		for (Entry<String, Integer> entry : allTextMonos) {
			allMonos.add(entry.getKey());
		}
		for (Entry<String, Integer> entry : allTextBis) {
			allBis.add(entry.getKey());
		}
		for (Entry<String, Integer> entry : allTextTris) {
			allTris.add(entry.getKey());
		}
		
		double[][] monoArr = new double[allMonos.size()][totalDocs];
		double[][] biArr = new double[allBis.size()][totalDocs];
		double[][] triArr = new double[allTris.size()][totalDocs];
		
		String[] allMonoArr =  allMonos.toArray(new String[allMonos.size()]);
		String[] allBiArr =  allBis.toArray(new String[allBis.size()]);
		String[] allTriArr = allTris.toArray(new String[allTris.size()]);
		
		Set<String> fileNameSet = ngramDocMap.keySet();
		Map<Integer, String> fileNameMap = new HashMap<Integer, String>();
		int i = 0;
		for (String fileName : fileNameSet) {
			fileNameMap.put(i, fileName);
			Map<String, SortedSet<Entry<String, Integer>>> ngMap = ngramDocMap.get(fileName);
			SortedSet<Entry<String, Integer>> monos = ngMap.get("mono");
			for (Entry<String, Integer> monoEntry : monos) {
				String mono = monoEntry.getKey();
				if (allMonos.contains(mono)) {
					monoArr[Arrays.binarySearch(allMonoArr, mono)][i] = monoEntry.getValue();
				}
			}
			SortedSet<Entry<String, Integer>> bis = ngMap.get("bi");
			for (Entry<String, Integer> biEntry : bis) {
				String bi = biEntry.getKey();
				if (allBis.contains(bi)) {
					biArr[Arrays.binarySearch(allBiArr, bi)][i] = biEntry.getValue();
				}
			}
			SortedSet<Entry<String, Integer>> tris = ngMap.get("tri");
			for (Entry<String, Integer> triEntry : tris) {
				String tri = triEntry.getKey();
				if (allTris.contains(tri)) {
					triArr[Arrays.binarySearch(allTriArr, tri)][i] = triEntry.getValue();
				}
			}
			i++;
		}
		Map<String, Map<String, Double>> monoKeywordMap = calculateTFIDF(monoArr, allMonoArr, fileNameMap);		
		Map<String, Map<String, Double>> biKeywordMap = calculateTFIDF(biArr, allBiArr, fileNameMap);
		Map<String, Map<String, Double>> triKeywordMap = calculateTFIDF(triArr, allTriArr, fileNameMap);	


		for (String fileName : monoKeywordMap.keySet()) {
			System.out.println("keywords for "+fileName);
			for (String keyword : monoKeywordMap.get(fileName)) {
				System.out.println(keyword);
			}
		}
		for (String fileName : biKeywordMap.keySet()) {
			System.out.println("keywords for "+fileName);
			for (String keyword : biKeywordMap.get(fileName)) {
				System.out.println(keyword);
			}
		}
		for (String fileName : triKeywordMap.keySet()) {
			System.out.println("keywords for "+fileName);
			for (String keyword : triKeywordMap.get(fileName)) {
				System.out.println(keyword);
			}
		}
	}
	/**
	 * @param bigArr
	 * @param allGs
	 * @param fileNameMap 
	 */
	private Map<String, Map<String, Double>> calculateTFIDF(double[][] bigArr, String[] allGs, Map<Integer, String> fileNameMap) {
		DoubleMatrix doubleMatrix = new DoubleMatrix(bigArr);
		int columns = doubleMatrix.columns;
		Map<String, Map<String, Double>> keywordMap = new HashMap<String, Map<String, Double>>();
		Map<Integer, Integer> maxFreqMap = new LinkedHashMap<Integer, Integer>();
		for (int i = 0; i < columns; i++) {
			DoubleMatrix aColumn = doubleMatrix.getColumn(i);
			int largestF = 0;
			for (int j = 0; j < aColumn.rows; j++) {
				int temp = (int) aColumn.get(j);
				if (temp>largestF) {
					largestF = temp;
				}
			}
			maxFreqMap.put(i, largestF);
		}
		for (int j = 0; j < bigArr.length; j++) {
			double counter = 0;
			int numOfDocs = bigArr[j].length;
			for (int k = 0; k < numOfDocs; k++) {
				if (bigArr[j][k]!=0) {
					counter ++;
				}
			}			
			for (int k = 0; k < numOfDocs; k++) {
				if (bigArr[j][k]!=0) {
					//double tf = Math.log(bigArr[j][k]+1);
					//double tf = bigArr[j][k];
					double tf = 0.4 + (0.6*bigArr[j][k])/(double)maxFreqMap.get(k);
					double idf = Math.log((double)numOfDocs/counter);
					if (tf*idf>3) {
						if (keywordMap.containsKey(fileNameMap.get(k))) {
							keywordMap.get(fileNameMap.get(k)).put(allGs[j],tf*idf);
						}
						else {
							Map<String, Double> keywordScoreMap= new HashMap<String, Double>();
							keywordScoreMap.put(allGs[j],tf*idf);
							keywordMap.put(fileNameMap.get(k), keywordScoreMap);
						}
						//System.out.println(fileNameMap.get(k)+" "+allGs[j]+" "+ tf*idf);
					}
				}
			}
		}
		return keywordMap;
	}
	public static void main(String[] args) throws IOException {
		TfIdf tfIdf = new TfIdf();
		tfIdf.calculateWordRarity();
	}
}
