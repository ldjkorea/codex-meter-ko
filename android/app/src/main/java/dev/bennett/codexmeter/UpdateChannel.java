package dev.bennett.codexmeter;

import java.util.List;

/**
 * Stable tracks non-prerelease releases; the retained "alpha" preference includes betas.
 * Korean APKs always advance versionCode. Channel selection never permits a downgrade or
 * equal-code reinstall; UpdateInstaller enforces the package and signing identity as well.
 */
public final class UpdateChannel {
    public static final String STABLE = "stable";
    public static final String ALPHA = "alpha";

    private UpdateChannel() {
    }

    public static String normalize(String value) {
        return ALPHA.equalsIgnoreCase(value == null ? "" : value.trim()) ? ALPHA : STABLE;
    }

    public static boolean isAlpha(String channel) {
        return ALPHA.equals(normalize(channel));
    }

    /** Newest release the channel tracks, independent of the installed version. */
    public static GitHubRelease trackedRelease(List<GitHubRelease> releases, String channel) {
        if (releases == null || releases.isEmpty()) {
            return null;
        }
        if (isAlpha(channel)) {
            return releases.get(0);
        }
        return GitHubReleaseParser.latestStable(releases);
    }

    /** Release to offer as an update for the installed version, or null when up to date. */
    public static GitHubRelease selectUpdate(List<GitHubRelease> releases,
            String installedVersion, String channel) {
        GitHubRelease tracked = trackedRelease(releases, channel);
        if (tracked == null) {
            return null;
        }
        if (tracked.isNewerThan(installedVersion)) {
            return tracked;
        }
        if (!isAlpha(channel) && isReturnToStable(tracked, installedVersion)) {
            return tracked;
        }
        return null;
    }

    /** Legacy SemVer channel hint. This does not override the monotonic installer checks. */
    public static boolean isReturnToStable(GitHubRelease release, String installedVersion) {
        if (release == null || release.prerelease) {
            return false;
        }
        ReleaseVersion installed = ReleaseVersion.parse(installedVersion);
        ReleaseVersion target = ReleaseVersion.parse(release.version);
        return installed != null && target != null && installed.isPrerelease()
                && target.compareTo(installed) < 0;
    }
}
