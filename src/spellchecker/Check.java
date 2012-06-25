package spellchecker;
import java.util.*;

public class Check {

	public static void main(String[] args) {
		
		FileCabinet cabinet = new FileCabinet("/Users/tunderwood/javaspellcheck/");
		double[][] confusionMatrix = cabinet.getMatrix();
		Levenshtein matcher = new Levenshtein(confusionMatrix);
		
		Tuple[] zipper = cabinet.getMainDictionary();
		int numWords = zipper.length;
		String[] mainDictionary = new String[numWords];
		double[] mainCoefficients = new double[numWords];
		for (int i = 0; i < numWords; ++i) {
			Tuple aPair = zipper[i];
			mainDictionary[i] = aPair.getFirst();
			mainCoefficients[i] = Math.log10(aPair.getSecond()) / 18;
		}
		
		String[] rawTokens = cabinet.wordsToProcess(100000, 110000);
		System.out.println(rawTokens.length);
		
		Scanner keyboard = new Scanner(System.in, "UTF8");
		
		RuleGuesser guessBot = new RuleGuesser(mainDictionary, mainCoefficients, matcher);
		String[] assignment = new String[9000];
		
		for (int i = 0; i < 9000; ++i) {
			assignment[i] = rawTokens[i];
		}
		
		guessBot.mineMatches(assignment);

	}
}
