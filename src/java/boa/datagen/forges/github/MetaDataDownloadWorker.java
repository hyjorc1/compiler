package boa.datagen.forges.github;

import java.io.File;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import boa.datagen.util.FileIO;
import gnu.trove.set.hash.THashSet;

public class MetaDataDownloadWorker implements Runnable {
	private TokenList tokens;
	private String repository_location;
	private final String output;
	JsonArray javarepos;
	private final String repo_url_header = "https://api.github.com/repos/";
	String stateFile = "";
	int Counter = 1;
	final static int RECORDS_PER_FILE = 100;
	final int startFileNumber;
	final int endFileNumber;
	THashSet<String> names = GitHubRepoMetaDataDownloader.names;

	public MetaDataDownloadWorker(String repoPath, String output, TokenList tokenList, int start, int end, int index) {
		this.output = output;
		this.tokens = tokenList;
		this.repository_location = repoPath;
		this.javarepos = new JsonArray();
		this.startFileNumber = start;
		this.endFileNumber = end;
	}

	public void downloadRepoMetaDataForRepoIn(int from, int to) {
		System.out.println(Thread.currentThread().getId() + " Responsible for processing: " + from + " and " + to);
		int pageNumber = from;
		Token tok = this.tokens.getNextAuthenticToken("https://api.github.com/repositories");
		File inDir = new File(repository_location);
		File[] files = inDir.listFiles();
		while (pageNumber < to) {
			File repoFile = files[pageNumber];
			String content = FileIO.readFileContents(repoFile);
			Gson parser = new Gson();
			JsonArray repos = parser.fromJson(content, JsonElement.class).getAsJsonArray();
			MetadataCacher mc = null;
			int size = repos.size();
			for (int j = 0; j < size; j++) {
				JsonObject rep = repos.get(j).getAsJsonObject();
				JsonObject repo = new JsonObject();
				String name = rep.get("full_name").getAsString();
				if (names.contains(name)) {
					names.remove(name);
					continue;
				}
				repo.addProperty("id", rep.get("id").getAsInt());
				repo.addProperty("full_name", name);
				String shortName = rep.get("name").getAsString();
				repo.addProperty("name", shortName);
				JsonObject langlist = rep.get("language_list").getAsJsonObject();
				repo.add("language_list", langlist);
				repo.addProperty("description", rep.get("description").getAsString());
				String repourl = this.repo_url_header + name;
				if (tok.getNumberOfRemainingLimit() <= 0) {
					tok = this.tokens.getNextAuthenticToken("https://api.github.com/repositories");
				}
				mc = new MetadataCacher(repourl, tok.getUserName(), tok.getToken());
				boolean authnticationResult = mc.authenticate();
				if (authnticationResult) {
					mc.getResponse();
					String pageContent = mc.getContent();
					JsonObject repository = parser.fromJson(pageContent, JsonElement.class).getAsJsonObject();
					int stars = repository.get("stargazers_count").getAsInt();
					repo.addProperty("stargazers_count", stars);
					int forks = repository.get("forks_count").getAsInt();
					repo.addProperty("forks_count", forks);
					String created = repository.get("created_at").getAsString();
					repo.addProperty("created_at", created);
					String homepage = "";
					if (!repository.get("homepage").isJsonNull())
						homepage = repository.get("homepage").getAsString();
					repo.addProperty("homepage", homepage);
					String html = repository.get("html_url").getAsString();
					repo.addProperty("html_url", html);
					addRepo(output, repo);
					tok.setLastResponseCode(mc.getResponseCode());
					tok.setnumberOfRemainingLimit(mc.getNumberOfRemainingLimit());
					tok.setResetTime(mc.getLimitResetTime());
				} else {
					final int responsecode = mc.getResponseCode();
					System.err.println("authentication error " + responsecode + " " + name);
					mc = new MetadataCacher("https://api.github.com/repositories", tok.getUserName(), tok.getToken());
					if (mc.authenticate()) {
						tok.setnumberOfRemainingLimit(mc.getNumberOfRemainingLimit());
					} else {
						System.out.println("token: " + tok.getId() + " exhausted");
						tok.setnumberOfRemainingLimit(0);
						j--;
					}
				}
			}
			pageNumber++;
			System.out.println(Thread.currentThread().getId() + " processing: " + pageNumber);
		}
		this.writeRemainingRepos(output);
		System.out.print(Thread.currentThread().getId() + "finished");
	}

	private void addRepo(String output, JsonObject repo) {
		File fileToWriteJson = null;
		this.javarepos.add(repo);
		if (this.javarepos.size() % RECORDS_PER_FILE == 0) {
			fileToWriteJson = new File(output + "Thread-" + Thread.currentThread().getId() + "-page-" + Counter + ".json");
			while (fileToWriteJson.exists()) {
				System.out.println("file thread-" + Thread.currentThread().getId() + "-page-" + Counter + " arleady exist");
				Counter++;
				fileToWriteJson = new File(output + "Thread-" + Thread.currentThread().getId() + "-page-" + Counter + ".json");
			}
			FileIO.writeFileContents(fileToWriteJson, this.javarepos.toString());
			System.out.println(Thread.currentThread().getId() + " " + Counter++);
			this.javarepos = new JsonArray();
		}
	}

	public void writeRemainingRepos(String output) {
		File fileToWriteJson = null;
		if (this.javarepos.size() > 0) {
			fileToWriteJson = new File(output + "/java/Thread-" + Thread.currentThread().getId() + "-page-" + Counter + ".json");
			while (fileToWriteJson.exists()) {
				Counter++;
				fileToWriteJson = new File(output + "/java/Thread-" + Thread.currentThread().getId() + "-page-" + Counter + ".json");
			}
			FileIO.writeFileContents(fileToWriteJson, this.javarepos.toString());
			System.out.println(Thread.currentThread().getId() + " java " + Counter++);
		}
	}

	@Override
	public void run() {
			this.downloadRepoMetaDataForRepoIn(this.startFileNumber, this.endFileNumber);
	}
}
