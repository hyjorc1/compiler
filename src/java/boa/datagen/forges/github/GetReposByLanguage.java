package boa.datagen.forges.github;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.corba.se.impl.orbutil.RepIdDelegator;

import boa.datagen.util.FileIO;

public class GetReposByLanguage {

	static int stars = 0;

	public static void main(String[] args) {
		TokenList tokens = new TokenList(args[0]);
		String outDir = args[1];
		String[] languages = args[2].split(",");
		stars = 1;

		if (args.length > 2) {
			languages = new String[args.length - 2];
			for (int i = 2; i < args.length; i++)
				languages[i - 2] = args[i];
		}
		Thread[] workers = new Thread[languages.length];
		for (int i = 0; i < languages.length; i++) {
			workers[i] = new Thread(new Worker(i, languages[i], outDir, tokens));
			workers[i].start();
		}
		for (Thread thread : workers)
			while (thread.isAlive())
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	}

	public static class Worker implements Runnable {
		private final int id;
		private final String language;
		private TokenList tokens;
		private final String outDir;
		private JsonArray repos = new JsonArray();
		private final int RECORDS_PER_FILE = 100;
		private int counter = 0;
		private HashSet<Integer> processedRepID = new HashSet<>();
		private ArrayList<Integer> IDtoWrite = new ArrayList<>();

		public Worker(int id, String language, String outDir, TokenList tokenList) {
			this.id = id;
			this.language = language;
			this.outDir = outDir;
			this.tokens = tokenList;
			File processedRepos = new File(outDir + "/" + language + "processed.txt");
			try {
				processedRepos.createNewFile();
				Files.lines(processedRepos.toPath()).forEach(repID -> processedRepID.add(Integer.parseInt(repID)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.DATE, 1);
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH) + 1; // month starts from 0
			int day = cal.get(Calendar.DAY_OF_MONTH);

			String monthString = month < 10 ? "0" + month : String.valueOf(month);
			String dayString = day < 10 ? "0" + day : String.valueOf(day);
			String time = year + "-" + monthString + "-" + dayString + "T23:59:59Z";

//			String time = "2018-12-21T01:01:01Z";
//			String time = year + "-" + month + "-" + day + "T23:59:59Z";
			Gson parser = new Gson();

			while (true) {
				Token tok = this.tokens.getNextAuthenticToken("https://api.github.com/repositories");
				String url = "https://api.github.com/search/repositories?q=language:" + language + "+stars:>=" + stars
						+ "+pushed:<=" + time + "&sort=updated&order=desc&per_page=100";
				System.out.println(url);
				MetadataCacher mc = new MetadataCacher(url, tok.getUserName(), tok.getToken());
				mc.authenticate();
				while (!mc.isAuthenticated() || mc.getNumberOfRemainingLimit() <= 0) {
					System.out.println("user: " + tok.getUserName() + " limit: " + mc.getNumberOfRemainingLimit());
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					mc = new MetadataCacher(url, tok.getUserName(), tok.getToken());
					mc.authenticate();
				}
				mc.getResponseJson();
				String content = mc.getContent();

				JsonObject json = null;
				json = parser.fromJson(content, JsonElement.class).getAsJsonObject();
				JsonArray items = json.getAsJsonArray("items");
				if (items.size() > 0) {
					for (int j = 0; j < items.size(); j++) {
						JsonObject item = items.get(j).getAsJsonObject();
						// check if repository is already saved
						int repID = item.get("id").getAsInt();
						if (!processedRepID.contains(repID)) {
							this.addRepo(item);
							processedRepID.add(repID);
							IDtoWrite.add(repID);
						} else {
							System.out.println(repID + " already written");
						}
						String pushed = item.get("pushed_at").getAsString();
						if (pushed.compareTo(time) < 0) {
							time = pushed;
						}
					}
				}
				int count = json.get("total_count").getAsInt();
				if (count == items.size())
					break;
				if (tok.getNumberOfRemainingLimit() <= 1) {
					long t = mc.getLimitResetTime() * 1000 - System.currentTimeMillis();
					if (t >= 0) {
						System.out.println("Waiting " + (t / 1000) + " seconds for sending more requests.");
						try {
							Thread.sleep(t);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			writeRemainingRepos();
		}

		private void addRepo(JsonObject repo) {
			File fileToWriteJson = null;
			this.repos.add(repo);
			if (this.repos.size() % RECORDS_PER_FILE == 0) {
				fileToWriteJson = new File(outDir + "/Thread-" + this.id + "-page-" + counter + ".json");
				while (fileToWriteJson.exists()) {
					System.out.println(fileToWriteJson.getAbsolutePath() + " arleady exist");
					counter++;
					fileToWriteJson = new File(outDir + "/Thread-" + this.id + "-page-" + counter + ".json");
				}
				FileIO.writeFileContents(fileToWriteJson, this.repos.toString());
				System.out.println(Thread.currentThread().getId() + " " + counter++);
				this.repos = new JsonArray();
				try (FileWriter fw = new FileWriter(outDir + "/" + language + "processed.txt", true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter out = new PrintWriter(bw)) {
					for (Integer repID : IDtoWrite) {
						out.println(repID.intValue());
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					IDtoWrite.clear();
				}
			}
		}

		public void writeRemainingRepos() {
			File fileToWriteJson = null;
			if (this.repos.size() > 0) {
				fileToWriteJson = new File(outDir + "/Thread-" + this.id + "-page-" + counter + ".json");
				while (fileToWriteJson.exists()) {
					counter++;
					fileToWriteJson = new File(outDir + "/Thread-" + this.id + "-page-" + counter + ".json");
				}
				FileIO.writeFileContents(fileToWriteJson, this.repos.toString());
				System.out.println(this.id + counter++);
			}
		}
	}
}
