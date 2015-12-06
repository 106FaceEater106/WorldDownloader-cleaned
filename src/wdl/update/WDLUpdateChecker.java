package wdl.update;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import wdl.WDL;
import wdl.WDLMessageTypes;
import wdl.WDLMessages;
import wdl.update.Release.HashData;

/**
 * Performs the update checking.
 */
public class WDLUpdateChecker extends Thread {
	private static volatile boolean shown = false;
	
	/**
	 * List of releases.  May be null if the checker has not finished.
	 */
	private static volatile List<Release> releases;
	
	/**
	 * The release that is currently running.
	 * 
	 * May be null.
	 */
	private static volatile Release runningRelease;
	/**
	 * Gets the current list of releases. May be null if the checker has not
	 * finished.
	 */
	public static List<Release> getReleases() {
		return releases;
	}
	
	/**
	 * Gets the current release.  May be null if the checker has not finished
	 * or if the current version isn't released.
	 */
	public static Release getRunningRelease() {
		return runningRelease;
	}
	
	/**
	 * Calculates the release that should be used based off of the user's options.
	 * 
	 * May be null if the checker has not finished.
	 */
	public static Release getRecomendedRelease() {
		if (releases == null) {
			return null;
		}
		if (runningRelease == null) {
			return null;
		}
		
		String mcVersion = WDL.getMinecraftVersion();
		
		boolean usePrereleases = WDL.globalProps.getProperty(
				"UpdateAllowBetas").equals("true");
		boolean versionMustBeExact = WDL.globalProps.getProperty(
				"UpdateMinecraftVersion").equals("client");
		boolean versionMustBeCompatible = WDL.globalProps.getProperty(
				"UpdateMinecraftVersion").equals("server");
		
		for (Release release : releases) {
			if (release.hiddenInfo != null) {
				if (release.prerelease && !usePrereleases) {
					continue;
				}
				
				if (versionMustBeExact) {
					if (!release.hiddenInfo.mainMinecraftVersion
							.equals(mcVersion)) {
						continue;
					}
				} else if (versionMustBeCompatible) {
					boolean foundCompatible = false;
					for (String version : release.
							hiddenInfo.supportedMinecraftVersions) {
						if (version.equals(mcVersion)) {
							foundCompatible = true;
							break;
						}
					}
					
					if (!foundCompatible) {
						continue;
					}
				}
				
				if (releases.indexOf(release) > releases.indexOf(runningRelease)) {
					//Too old
					continue;
				}
				
				return release;
			}
		}
		
		return null;
	}
	
	/**
	 * Is there a new version that should be used?
	 * 
	 * True if the running release is not null and if the recommended
	 * release is not the running release.
	 * 
	 * The return value of this method may change as the update checker
	 * runs.
	 */
	public static boolean hasNewVersion() {
		if (runningRelease == null) {
			return false;
		}
		Release recomendedRelease = getRecomendedRelease();
		if (recomendedRelease == null) {
			return false;
		}
		return runningRelease != recomendedRelease;
	}
	
	/**
	 * Call once the world has loaded.  Will check and start a new update checker
	 * if needed.
	 */
	public static void startIfNeeded() {
		if (!shown) {
			shown = true;
			
			new WDLUpdateChecker().start();
		}
	}
	
	private static final String FORUMS_THREAD_USAGE_LINK = "http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2520465#Usage";
	private static final String GITHUB_LINK = "https://github.com/pokechu22/WorldDownloader";
	private static final String REDISTRIBUTION_LINK = "http://pokechu22.github.io/WorldDownloader/redistribution";
	private static final String SMR_LINK = "http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/mods-discussion/2314237";
	
	private WDLUpdateChecker() {
		super("WorldDownloader update check thread");
	}
	
	@Override
	public void run() {
		try {
			if (!WDL.globalProps.getProperty("TutorialShown").equals("true")) {
				sleep(5000);
				
				ChatComponentTranslation success = new ChatComponentTranslation(
						"wdl.intro.success");
				ChatComponentTranslation mcfThread = new ChatComponentTranslation(
						"wdl.intro.forumsLink");
				mcfThread.getChatStyle().setColor(EnumChatFormatting.BLUE)
						.setUnderlined(true).setChatClickEvent(
								new ClickEvent(Action.OPEN_URL,
										FORUMS_THREAD_USAGE_LINK));
				ChatComponentTranslation usage = new ChatComponentTranslation(
						"wdl.intro.usage", mcfThread);
				ChatComponentTranslation githubRepo = new ChatComponentTranslation(
						"wdl.intro.githubRepo");
				githubRepo.getChatStyle().setColor(EnumChatFormatting.BLUE)
						.setUnderlined(true).setChatClickEvent(
								new ClickEvent(Action.OPEN_URL,
										GITHUB_LINK));
				ChatComponentTranslation contribute = new ChatComponentTranslation(
						"wdl.intro.contribute", githubRepo);
				ChatComponentTranslation redistributionList = new ChatComponentTranslation(
						"wdl.intro.redistributionList");
				redistributionList.getChatStyle().setColor(EnumChatFormatting.BLUE)
						.setUnderlined(true).setChatClickEvent(
								new ClickEvent(Action.OPEN_URL,
										REDISTRIBUTION_LINK));
				ChatComponentTranslation warning = new ChatComponentTranslation(
						"wdl.intro.warning");
				warning.getChatStyle().setColor(EnumChatFormatting.DARK_RED)
						.setBold(true);
				ChatComponentTranslation illegally = new ChatComponentTranslation(
						"wdl.intro.illegally");
				illegally.getChatStyle().setColor(EnumChatFormatting.DARK_RED)
						.setBold(true);
				ChatComponentTranslation stolen = new ChatComponentTranslation(
						"wdl.intro.stolen", warning, redistributionList, illegally);
				ChatComponentTranslation smr = new ChatComponentTranslation(
						"wdl.intro.stopModReposts");
				smr.getChatStyle().setColor(EnumChatFormatting.BLUE)
						.setUnderlined(true).setChatClickEvent(
								new ClickEvent(Action.OPEN_URL,
										SMR_LINK));
				ChatComponentTranslation stolenBeware = new ChatComponentTranslation(
						"wdl.intro.stolenBeware", smr);
				
				WDLMessages.chatMessage(WDLMessageTypes.UPDATES, success);
				WDLMessages.chatMessage(WDLMessageTypes.UPDATES, usage);
				WDLMessages.chatMessage(WDLMessageTypes.UPDATES, contribute);
				WDLMessages.chatMessage(WDLMessageTypes.UPDATES, stolen);
				WDLMessages.chatMessage(WDLMessageTypes.UPDATES, stolenBeware);
				
				WDL.globalProps.setProperty("TutorialShown", "true");
				WDL.saveGlobalProps();
			}
			
			sleep(5000);
			
			releases = GithubInfoGrabber.getReleases();
			WDL.chatMessage(WDLMessageTypes.UPDATE_DEBUG, "Found " + releases.size()
					+ " releases.");
			for (int i = 0; i < releases.size(); i++) {
				Release release = releases.get(i);
				
				if (release.tag.equalsIgnoreCase(WDL.VERSION)) {
					runningRelease = release;
				}
			}
			
			if (runningRelease == null) {
				WDL.chatMessage(WDLMessageTypes.UPDATE_DEBUG, "Could not find a release "
						+ "for " + WDL.VERSION + ".  You may be running a "
						+ "version that hasn't been released yet.");
				return;
			}
			
			if (hasNewVersion()) {
				Release recomendedRelease = getRecomendedRelease();
				
				WDL.chatMessage(WDLMessageTypes.UPDATES, "New version avaiable!  Newest " 
						+ "version is " + recomendedRelease.tag + ".  You are "
						+ "running " + runningRelease.tag + ".");
				//TODO: add a download link.
			}
			
			if (runningRelease.hiddenInfo == null) {
				WDL.chatMessage(WDLMessageTypes.UPDATE_DEBUG, "Could not find hidden "
						+ "data for release.  Skipping hashing.");
				return;
			}
			//Check the hashes, and list any failing ones.
			Map<HashData, Object> failed = new HashMap<HashData, Object>();
			
			hashLoop: for (HashData data : runningRelease.hiddenInfo.hashes) {
				try {
					String hash = ClassHasher.hash(data.relativeTo, data.file);
					
					for (String validHash : data.validHashes) {
						if (validHash.equalsIgnoreCase(hash)) {
							// Labeled continues / breaks _are_ a thing.
							// This just continues the outer loop.
							continue hashLoop;
						}
					}
					
					WDL.chatMessage(
							WDLMessageTypes.UPDATE_DEBUG,
							"Bad hash for " + data.file + " (relative to "
									+ data.relativeTo + "): Expected "
									+ Arrays.toString(data.validHashes)
									+ ", got " + hash);
					
					failed.put(data,  hash);
					continue;
				} catch (Exception e) {
					WDL.chatMessage(WDLMessageTypes.UPDATE_DEBUG, "Bad hash for "
							+ data.file + " (relative to " + data.relativeTo
							+ "): Exception: " + e);
					
					failed.put(data,  e);
				}
			}
			
			if (failed.size() > 0) {
				WDL.chatMessage(WDLMessageTypes.UPDATES, "§cSome files have invalid " +
						"hashes!  Your installation may be corrupt or " +
						"compromised.  If you are running a custom build, " +
						"this is normal.");
			}
		} catch (Exception e) {
			WDL.chatMessage(WDLMessageTypes.UPDATE_DEBUG, "Failed to perform update check: "
					+ e.toString());
			e.printStackTrace();
		}
	}
}
