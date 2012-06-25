package spellchecker;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;

public class RuleGuesser {
	String[] dictionary;
	int[] dictBigramCount;
	double[] freqCoefficients;
	Levenshtein matcher;
	HashSet<String> dictionarySet;
	static final String alphabet ="abcdefghijklmnopqrstuvwxyz'$";
	int[] alphaTranslationTable;
	ArrayList<ArrayList<HashSet<Integer>>> bigramIndex;
	// Yes, you got that right. A list of lists, of hashsets containing integers.
	
	public RuleGuesser(String[] dictionary, double[] freqCoefficients, Levenshtein matcher) {
		this.dictionary = dictionary;
		this.freqCoefficients = freqCoefficients;
		this.matcher = matcher;
		
		dictionarySet = new HashSet<String>();
		for (String word: dictionary) {
			dictionarySet.add(word);
		}
		
		alphaTranslationTable = new int[256];
		// Initialize the translation table for k-gram indexing, using fixed alphabet.
		// The apostrophe, #26, is an all-purpose code for nonalphabetic characters.
		for (int i = 0; i < 256; ++i) {
			alphaTranslationTable[i] = 26;
		}
		// Now assign characters in the alphabet.
		for (int i = 0; i < alphabet.length(); ++i) {
			int ascii = (int) alphabet.charAt(i);
			alphaTranslationTable[ascii] = i;
			if (i < 26) {
				// Upper-case equivalent for alphabetic characters.
				int upper = ascii - 32;
				alphaTranslationTable[upper] = i;
			}
		}
		
		// Now we create a list of lists of hashsets, containing integers.
		bigramIndex = new ArrayList<ArrayList<HashSet<Integer>>>();
		for (int i = 0; i < 28; ++i) {
			ArrayList<HashSet<Integer>> aListOfSets = new ArrayList<HashSet<Integer>>();
			for (int j = 0; j < 28; ++j) {
				HashSet<Integer> wordsContainingBigramIJ = new HashSet<Integer>();
				aListOfSets.add(wordsContainingBigramIJ);
			}
			bigramIndex.add(aListOfSets);
		}
		
		dictBigramCount = new int[dictionary.length];
		for (int i = 0; i < dictionary.length; ++i) {
			String word = "$" + dictionary[i] + "$";
			HashSet<String> bigrams = new HashSet<String>();
			for (int j = 1; j < word.length(); ++ j) {
				String bigram = word.substring(j - 1, j + 1);
				bigrams.add(bigram);
				int gram1 = bigramcode(word.charAt(j - 1));
				int gram2 = bigramcode(word.charAt(j));
				ArrayList<HashSet<Integer>> listOfSets = bigramIndex.get(gram1);
				HashSet<Integer> setOfWords = listOfSets.get(gram2);
				setOfWords.add(i);
			}
			dictBigramCount[i] = bigrams.size();
		}	
	}
	
	private static boolean isUpper(char toTest) {
		int unicodeVal = (int) toTest;
		boolean testCondition = false;
		if (unicodeVal > 64 & unicodeVal < 91) testCondition = true;
		return testCondition;
	}
	
	private static boolean isTitlecase(String toTest) {
		if (toTest.length() < 2) return false;
		char initial = toTest.charAt(0);
		if (!isUpper(initial)) return false;
		for (int i = 1; i < toTest.length(); ++i) {
			char bodyChar = toTest.charAt(i);
			if (isUpper(bodyChar)) return false;
		}
		return true;
	}
	private static boolean largelyNumeric(String toTest) {
		if (toTest.length() < 2) return false;
		int digits = 0;
		for (int i = 0; i < toTest.length(); ++i) {
			int ascii = (int) toTest.charAt(i);
			if (ascii >= 48 & ascii <= 57) ++ digits;
		}
		double percent = digits / (double) toTest.length();
		if (percent > 0.3) return true;
		else return false;
	}
	
	private int bigramcode(char aChar) {
		int code = (int) aChar;
		if (code > 255) return 26;
		else return alphaTranslationTable[code];
	}
	
	private HashSet<Integer> wordsContaining(String bigram){
		int gram1 = bigramcode(bigram.charAt(0));
		int gram2 = bigramcode(bigram.charAt(1));
		ArrayList<HashSet<Integer>> aListOfSets = bigramIndex.get(gram1);
		return aListOfSets.get(gram2);
	}
	
	private ArrayList<Integer> getCandidates(String word) {
		word = "$" + word + "$";
		int[] sumMatchesPerWord = new int[dictionary.length];
		Arrays.fill(sumMatchesPerWord, 0);
		
		HashSet<String> bigrams = new HashSet<String>();
		for (int j = 1; j < word.length(); ++ j) {
			String bigram = word.substring(j - 1, j + 1);
			bigrams.add(bigram);
		}
		int wordBigramCount = bigrams.size();
		for (String bigram : bigrams) {
			HashSet<Integer> wordsMatchingThisBigram = wordsContaining(bigram);
			for (int match : wordsMatchingThisBigram) {
				++ sumMatchesPerWord[match];
			}
		}
		ArrayList<Integer> candidates = new ArrayList<Integer>();
		for (int i = 0; i < dictionary.length; ++i) {
			double dice = (2 * sumMatchesPerWord[i]) / (double) (wordBigramCount + dictBigramCount[i]);
			if (dice > 0.3) candidates.add(i);
		}
		return candidates;		
	}
	
	private ArrayList<String> matchtail(String word) {
		// Identifies dictionary words of which "word" is a possible
		// fragment, to disqualify it for usual spellchecking.
		int[] possibleMatches = new int[dictionary.length];
		Arrays.fill(possibleMatches, 0);
		String lastBigram = word.charAt(word.length() - 1) + "$";
		String nexttolastBigram = word.substring((word.length() -2 ), word.length());
		HashSet<Integer> wordsMatchingThisBigram = wordsContaining(lastBigram);
		for (int match : wordsMatchingThisBigram) {
			++ possibleMatches[match];
		}
		wordsMatchingThisBigram = wordsContaining(nexttolastBigram);
		for (int match : wordsMatchingThisBigram) {
			++ possibleMatches[match];
		}
		ArrayList<String> tailwords = new ArrayList<String>();
		for (int i = 0; i < dictionary.length; ++i) {
			if (possibleMatches[i] > 1 ) tailwords.add(dictionary[i]);
		}
		ArrayList<String> tailmatches = new ArrayList<String>();
		for (String candidate : tailwords) {
			// We don't want to consider candidates that are shorter than the word,
			// or the same length. We're looking for words *of which* this is a fragment.
			// Also, if the candidate is only one letter longer, that might be considered
			// a valid correction and will not be disqualified.
			if (candidate.length() <= (word.length() - 1)) continue;
			String candidateChunk = candidate.substring(candidate.length() - word.length());
			if (candidateChunk.equals(word)) tailmatches.add(candidate);
		}
		return tailmatches;
	}
	
	public void mineMatches(String[] wordsToMatch) {
		for (String word: wordsToMatch) {
			String lowerWord = word.toLowerCase();
			// Reasons not to bother with this word.
			if (word.length() < 3) continue;
			if (word.contains("-")) continue;
			if (isTitlecase(word)) continue;
			if (dictionarySet.contains(lowerWord)) continue;
			if (dictionarySet.contains(word)) continue;
			if (largelyNumeric(word)) continue;
			
			String lasttwo = lowerWord.substring(lowerWord.length() - 2);
			boolean possessive = false;
			boolean plural = false;
			if (lasttwo.equals("'s")) {
				possessive = true;
				word = word.substring(0, word.length() -2 );
			}
			
			// Okay, let's test it.
			ArrayList<String> tailmatches = matchtail(word);
			if (tailmatches.size() > 0) {
				System.out.println(word + " could be a fragment of " + tailmatches.get(0));
				continue;
			}
			ArrayList<Integer> candidates = getCandidates(word);
			String[] wordsToCheck = new String[candidates.size()];
			double[] wordCoefficients = new double[candidates.size()];
			int idx = 0;
			for (int candidate : candidates) {
				wordsToCheck[idx] = dictionary[candidate];
				wordCoefficients[idx] = freqCoefficients[candidate];
				++ idx;
			}
			Tuple[] results = matcher.findMatches(word, wordsToCheck, wordCoefficients);
			Tuple first = results[0];
			Tuple second = results[1];
			String resultword = first.getFirst();
			String secondbest = second.getFirst();
			if (lowerWord.equals(resultword + "s")) {
				plural = true;
			}
			if (possessive != true & plural != true) {
				System.out.println(word + ": " + resultword + " " + secondbest);
				if (!resultword.equals("no match")) {
					matcher.traceSubstitutions(word, resultword);
				}
			}
			else if (possessive) System.out.println(lowerWord + ": " + resultword + " + POSSESSIVE");
			else if (plural) System.out.println(lowerWord + ": " + resultword + " + PLURAL");
		}
		matcher.listInsertions();
	}
}
