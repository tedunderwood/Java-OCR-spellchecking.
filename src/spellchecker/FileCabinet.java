package spellchecker;
import java.util.HashMap;

import static java.util.Arrays.fill;

public class FileCabinet {
	
	String inPath;
	String outPath;
	static final CharSequence dictionaryCharacters = "abcdefghijklmnopqrstuvwxyz0123456789ßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ'-÷";
	HashMap<Character, Integer> dictionaryMap;
	
	public FileCabinet(String rootPath) {
		inPath = rootPath + "infiles/";
		outPath = rootPath + "outfiles/";
		dictionaryMap = new HashMap<Character, Integer>();
		
		for (int i = 0; i < dictionaryCharacters.length(); i++) {
			char aCharacter = dictionaryCharacters.charAt(i);
			dictionaryMap.put(aCharacter, i);
		}
		for (int i = 0; i < 256; i ++) {
			char aCharacter = (char) i;
			Integer inYet = dictionaryMap.get(aCharacter);
			if (inYet == null) dictionaryMap.put(aCharacter, 70);
			if (i > 64 & i < 91) dictionaryMap.put(aCharacter, i - 65);
		}
	}
	
	private char codeToDictchar(int code) {
		char toReturn = '÷';
		if (code >= 0 & code <= 70) toReturn = dictionaryCharacters.charAt(code);
		return toReturn;
	}
	
	private int dictcharToCode(char dictchar) {
		Integer code = dictionaryMap.get(dictchar);
		if (code == null) code = 70;
		return code;
	}
	
	private static double stringToDouble(String aString) {
		try {
	         double d = Double.valueOf(aString.trim()).doubleValue();
	         return d;
	      } catch (NumberFormatException nfe) {
	         System.out.println("NumberFormatException: " + nfe.getMessage());
	         return 0.01;
	      }
	}
	
	public double[][] getMatrix() {
		LineReader inFile = new LineReader(inPath + "CharMatrix.txt");
		String[] fileLines = inFile.readlines();
		
		int[][] substitutionCounts = new int[256][71];
		
		for (int i = 0; i < 256; ++i) {
			String line = fileLines[i];
			String[] commaSepValues = line.split(",");
			int j = 0;
			
			for (String aValue : commaSepValues) {
				int substitutions = Integer.parseInt(aValue);
				substitutionCounts[i][j] = substitutions;
				++ j;
			}	
			++ i;
		}
		
		int[] substitutionSums = new int[256];
		fill(substitutionSums, 1);
		
		for (int i = 0; i < 256; ++ i) {
			for (int j = 0; j < 71; ++ j) {
				substitutionSums[i] += substitutionCounts[i][j];
			}
		}
		
		double[][] confusionMatrix = new double[256][71];
		
		for (int i = 0; i < 256; ++ i) {
			for (int j = 0; j < 71; ++ j) {
				confusionMatrix[i][j] = 1 - (substitutionCounts[i][j] / (double) substitutionSums[i]);
			}
		}
		// The confusion distance of a character to itself is defined as always zero.
		// We don't do this for dictionary character 70, because it's a catch-all.
		for (int i = 0; i < 256; ++i) {
			int dictionaryEquivalent = dictcharToCode((char) i);
			if (dictionaryEquivalent < 70) confusionMatrix[i][dictionaryEquivalent] = 0;
		}
		// We define the confusion distance from a diacritic to its normal equivalent as 0.1.
		CharSequence diacriticString = "èéêë";
		for (int i = 0; i < diacriticString.length(); ++i) {
			int diacriticDict = dictcharToCode(diacriticString.charAt(i));
			int diacriticText = (int) diacriticString.charAt(i);
			int normalText = (int) 'e';
			int normalDict = dictcharToCode('e');
			confusionMatrix[normalText][diacriticDict] = 0.1;
			confusionMatrix[diacriticText][normalDict] = 0.1;
		}
		diacriticString = "àáâãäå";
		for (int i = 0; i < diacriticString.length(); ++i) {
			int diacriticDict = dictcharToCode(diacriticString.charAt(i));
			int diacriticText = (int) diacriticString.charAt(i);
			int normalText = (int) 'a';
			int normalDict = dictcharToCode('a');
			confusionMatrix[normalText][diacriticDict] = 0.1;
			confusionMatrix[diacriticText][normalDict] = 0.1;
		}
		diacriticString = "ìíîï";
		for (int i = 0; i < diacriticString.length(); ++i) {
			int diacriticDict = dictcharToCode(diacriticString.charAt(i));
			int diacriticText = (int) diacriticString.charAt(i);
			int normalText = (int) 'i';
			int normalDict = dictcharToCode('i');
			confusionMatrix[normalText][diacriticDict] = 0.1;
			confusionMatrix[diacriticText][normalDict] = 0.1;
		}
		diacriticString = "òóôõöø";
		for (int i = 0; i < diacriticString.length(); ++i) {
			int diacriticDict = dictcharToCode(diacriticString.charAt(i));
			int diacriticText = (int) diacriticString.charAt(i);
			int normalText = (int) 'o';
			int normalDict = dictcharToCode('o');
			confusionMatrix[normalText][diacriticDict] = 0.1;
			confusionMatrix[diacriticText][normalDict] = 0.1;
		}
		diacriticString = "ùúûü";
		for (int i = 0; i < diacriticString.length(); ++i) {
			int diacriticDict = dictcharToCode(diacriticString.charAt(i));
			int diacriticText = (int) diacriticString.charAt(i);
			int normalText = (int) 'u';
			int normalDict = dictcharToCode('u');
			confusionMatrix[normalText][diacriticDict] = 0.1;
			confusionMatrix[diacriticText][normalDict] = 0.1;
		}
		return confusionMatrix;
	}
	
	public Tuple[] getMainDictionary() {
		LineReader inFile = new LineReader(inPath + "MainDictionary.txt");
		String[] fileLines = inFile.readlines();
		int numLines = fileLines.length;
		Tuple[] zipper = new Tuple[numLines];
		
		for (int i = 0; i < numLines; ++i) {
			String line = fileLines[i];
			String[] lineParts = line.split("[\t]");
			String word = lineParts[0];
			double freq = stringToDouble(lineParts[1]);
			Tuple aPair = new Tuple(word, freq);
			zipper[i] = aPair;
		}
		return zipper;
	}
	
	public String[] wordsToProcess(int start, int end) {
		LineReader inFile = new LineReader(inPath + "bigindex.txt");
		String[] fileLines = inFile.readslice(start, end);
		int numLines = fileLines.length;
		String[] returnList = new String[numLines];
		
		for (int i = 0; i < numLines; ++i) {
			String line = fileLines[i];
			String[] lineParts = line.split("[\t]");
			String word = lineParts[0];
			returnList[i] = word;
		}
		return returnList;
	}
}
