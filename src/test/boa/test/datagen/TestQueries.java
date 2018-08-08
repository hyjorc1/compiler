package boa.test.datagen;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import boa.datagen.util.FileIO;
import boa.evaluator.BoaEvaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


public class TestQueries {

	@Test
	public void testBugFix() {
		try {
			Process p = Runtime.getRuntime().exec("./boa.sh -e "
					+ "-i test/known-good/bug-fix.boa "
					+ "-d test/datagen/test_datagen "
					+ "-o test/datagen/Bug-fix_output");
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String s = null;
			 while ((s = stdInput.readLine()) != null) {
	                System.out.println(s);
				 p.waitFor();
	            }
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String expected = "AddedNullCheck[] = 1\n";
		File outputDir = new File("test/datagen/Bug-fix_output");
		String actual = getResults(outputDir);// evaluator.getResults();
		assertEquals(expected, actual);
	}
	
	public String getResults(File outputDir) {
		for (final File f : outputDir.listFiles()) {
			if (f.getName().startsWith("part")) {
				return FileIO.readFileContents(f);
			}
		}
		return "";
	}
}
