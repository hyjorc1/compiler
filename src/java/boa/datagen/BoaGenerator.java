/*
 * Copyright 2015, Hridesh Rajan, Robert Dyer,
 *                 and Iowa State University of Science and Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package boa.datagen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import boa.datagen.forges.github.GetGithubRepoByUser;
import boa.datagen.forges.github.LocalGitSequenceGenerator;
import boa.datagen.forges.github.MetaDataMaster;
import boa.datagen.util.Properties;

/**
 * The main entry point for Boa tools for generating datasets.
 *
 * @author hridesh
 * 
 */
public class BoaGenerator {
	private static boolean jsonAvailable = true;
	private static boolean tokenAvailable = false;

	public static void main(final String[] args) throws IOException {
		final Options options = new Options();
		BoaGenerator.addOptions(options);

		final CommandLine cl;
		try {
			cl = new PosixParser().parse(options, args);
		} catch (final org.apache.commons.cli.ParseException e) {
			System.err.println(e.getMessage());
			new HelpFormatter().printHelp("BoaCompiler", options);
			return;
		}
		BoaGenerator.handleCmdOptions(cl, options, args);
		
		String projectsPath = "/work/LAS/hridesh-lab/yijia/sutton_dataset/projects.txt";
		DefaultProperties.projects = getProjects(new File(projectsPath));

		/*
		 * 1. if user provides local json files 
		 * 2. if user provides username and password 
		 * in both the cases json files are going to be available
		 */

		if (jsonAvailable) {
			try {
				SeqRepoImporter.main(new String[0]);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			SeqCombiner.main(new String[0]);
		} else if (tokenAvailable) { // when user provides local repo and does
										// not have json files
			MetaDataMaster mdm = new MetaDataMaster();
			mdm.downloadRepoNames(DefaultProperties.TOKEN, DefaultProperties.OUTPUT);

			SeqCombiner.main(new String[0]);
		} else { // when user provides local repo and does not have json files
			File output = new File(DefaultProperties.OUTPUT);
			if (!output.exists())
				output.mkdirs();
			LocalGitSequenceGenerator.localGitSequenceGenerate(DefaultProperties.GH_GIT_PATH, DefaultProperties.OUTPUT);
			try {
				MapFileGen.main(new String[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		clear();
		
		String base = Properties.getProperty("output.path", DefaultProperties.OUTPUT);
		if (DefaultProperties.exceptions.size() != 0) {
			writeTo(base + "/exceptions_"+System.currentTimeMillis()+".txt");
		}
		if (DefaultProperties.processedProjects.size() != 0) {
			writeProcessedProjects(base + "/processed_"+System.currentTimeMillis()+".txt");
		}
	}
	
	public static HashSet<String> getProjects(File file) {
		if (!file.exists())
			return new HashSet<String>();
		HashSet<String> set = new HashSet<String>();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				set.add(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return set;
	}
	
	public static void writeTo(String path) throws IOException {
		FileWriter fw = new FileWriter(path);
		BufferedWriter bw = new BufferedWriter(fw);
		for (Entry<String, String> entry : DefaultProperties.exceptions.entrySet()) {
			bw.write(entry.getKey() + " " + entry.getValue());
			bw.newLine();
		}
		bw.close();
	}
	
	public static void writeProcessedProjects(String path) throws IOException {
		FileWriter fw = new FileWriter(path);
		BufferedWriter bw = new BufferedWriter(fw);
		for (String s : DefaultProperties.processedProjects) {
			bw.write(s);
			bw.newLine();
		}
		bw.close();
	}

	private static final void printHelp(Options options, String message) {
		String header = "The most commonly used Boa options are:";
		String footer = "\nPlease report issues at http://www.github.com/boalang/";
		System.err.println(message);
		new HelpFormatter().printHelp("boa", header, options, footer);
	}

	private static final void printHelp(Options options) {
		String header = "The most commonly used Boa options are:";
		String footer = "\nPlease report issues at http://www.github.com/boalang/";
		new HelpFormatter().printHelp("boa", header, options, footer);
	}

	private static void addOptions(Options options) {
		options.addOption("inputJson", "json", true, ".json files for metadata");
		options.addOption("inputToken", "token", true, "token file");
		options.addOption("inputRepo", "json", true, "cloned repo path");
		options.addOption("threads", "threads", true, "number of threads");
		options.addOption("projects", "projects", true, "maximum number of projects per sequence file");
		options.addOption("commits", "commits", true, "maximum number of commits of a project to be stored in the project object");
		options.addOption("nocommits", "nocommits", false, "do not store commits");	
		options.addOption("noasts", "noasts", false, "do not store asts");
		options.addOption("factor", "factor", true, "max size factor");
		options.addOption("exceptions", "exceptions", true, "do not generate those projects");
		options.addOption("size", "size", true, "maximum size of a project object to be stored");
		options.addOption("libs", "libs", true, "directory to store libraries");
		options.addOption("output", "output", true, "directory where output is desired");
		options.addOption("user", "user", true, "github username to authenticate");
		options.addOption("password", "password", true, "github password to authenticate.");
		options.addOption("targetUser", "targetUser", true, "username of target repository");
		options.addOption("targetRepo", "targetRepo", true, "name of the target repository");
		options.addOption("cache", "cache", false, "enable if you want to delete the cloned code for user.");
		options.addOption("debug", "debug", false, "enable for debug mode.");
		options.addOption("debugparse", "debugparse", false, "enable for debug mode when parsing source files.");
		options.addOption("help", "help", false, "help");
	}

	private static void handleCmdOptions(CommandLine cl, Options options, final String[] args) {
		if (cl.hasOption("inputJson") && cl.hasOption("inputRepo") && cl.hasOption("output")) {
			DefaultProperties.GH_JSON_PATH = cl.getOptionValue("inputJson");
			DefaultProperties.OUTPUT = cl.getOptionValue("output");
			// DefaultProperties.GH_GIT_PATH = GH_JSON_CACHE_PATH + "/github";
			DefaultProperties.GH_GIT_PATH = cl.getOptionValue("inputRepo");
		} else if (cl.hasOption("inputJson") && cl.hasOption("output")) {
			DefaultProperties.GH_JSON_PATH = cl.getOptionValue("inputJson");
			DefaultProperties.OUTPUT = cl.getOptionValue("output");
			DefaultProperties.GH_GIT_PATH = cl.getOptionValue("output");
		} else if (cl.hasOption("inputToken") && cl.hasOption("inputRepo") && cl.hasOption("output")) {
			DefaultProperties.TOKEN = cl.getOptionValue("inputToken");
			DefaultProperties.OUTPUT = cl.getOptionValue("output");
			// DefaultProperties.GH_GIT_PATH = GH_JSON_CACHE_PATH + "/github";
			DefaultProperties.GH_GIT_PATH = cl.getOptionValue("inputRepo");
			jsonAvailable = false;
			tokenAvailable = true;
		} else if (cl.hasOption("inputRepo") && cl.hasOption("output")) {
			DefaultProperties.OUTPUT = cl.getOptionValue("output");
			DefaultProperties.GH_GIT_PATH = cl.getOptionValue("inputRepo");
			jsonAvailable = false;
		} else if (cl.hasOption("user") && cl.hasOption("password") && cl.hasOption("targetUser")
				&& cl.hasOption("targetRepo") && cl.hasOption("output")) {
			try {
				// because there is no input directory in this case, we need to
				// create one
				String GH_JSON_PATH = new java.io.File(".").getCanonicalPath();
				DefaultProperties.GH_JSON_PATH = GH_JSON_PATH + "/input";
				getGithubMetadata(DefaultProperties.GH_JSON_PATH, cl.getOptionValue("user"),
						cl.getOptionValue("password"), cl.getOptionValue("targetUser"),
						cl.getOptionValue("targetRepo"));

				// output directory
				final String GH_JSON_CACHE_PATH = cl.getOptionValue("output");
				DefaultProperties.OUTPUT = GH_JSON_CACHE_PATH;
				DefaultProperties.GH_GIT_PATH = GH_JSON_CACHE_PATH + "/github";
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (cl.hasOption("help")) {
			String message = cl.getOptionValue("help");
			printHelp(options, message);
		} else {
			System.err.println("User must specify the path of the repository. Please see --remote and --local options");
			printHelp(options);
		}
		if (cl.hasOption("threads")) {
			DefaultProperties.NUM_THREADS = cl.getOptionValue("threads");
		}
		if (cl.hasOption("projects")) {
			DefaultProperties.MAX_PROJECTS = cl.getOptionValue("projects");
		}
		if (cl.hasOption("commits")) {
			DefaultProperties.MAX_COMMITS = cl.getOptionValue("commits");
		}
		if (cl.hasOption("size")) {
			DefaultProperties.MAX_SIZE_FOR_PROJECT_WITH_COMMITS = cl.getOptionValue("size");
		}
		if (cl.hasOption("debug")) {
			DefaultProperties.DEBUG = true;
		}
		if (cl.hasOption("debugparse")) {
			DefaultProperties.DEBUGPARSE = true;
		}
		if (cl.hasOption("cache")) {
			DefaultProperties.CACHE = true;
		}
		if (cl.hasOption("libs")) {
			DefaultProperties.CLASSPATH_ROOT = cl.getOptionValue("libs");
		}
		if (cl.hasOption("nocommits"))
			DefaultProperties.STORE_COMMITS = false;
		if (cl.hasOption("noasts")) {
			DefaultProperties.STORE_ASTS = false;
		}
		if (cl.hasOption("exceptions")) {
			try {
				DefaultProperties.exceptions = getExcludes(cl.getOptionValue("exceptions"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (cl.hasOption("factor")) {
			DefaultProperties.MAX_SIZE_FACTOR = Double.parseDouble(cl.getOptionValue("factor"));
		}
	}

	private static HashMap<String, String> getExcludes(String path) throws IOException {
		String line;
		File file = new File(path);
		HashMap<String, String> map = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		while ((line = br.readLine()) != null)
			map.put(line, "");
		br.close();
		return map;
	}

	//
	private static void clear() {
		File inputDirectory = new File(DefaultProperties.OUTPUT + "/buf-map");
		if (inputDirectory.exists())
			org.apache.commons.io.FileUtils.deleteQuietly(inputDirectory);
	}

	private static void getGithubMetadata(String inputPath, String username, String password, String targetUser,
			String targetRepo) {
		String[] args = { inputPath, username, password, targetUser, targetRepo };
		GetGithubRepoByUser.main(args);
	}
}
