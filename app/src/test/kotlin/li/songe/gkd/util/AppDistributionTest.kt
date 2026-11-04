package li.songe.gkd.util

import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class AppDistributionTest {
    @Test fun forkEndpointsStayInTheForkRepository() {
        assertEquals("https://github.com/STC214/GKD_", REPOSITORY_URL)
        assertEquals("$REPOSITORY_URL/issues", ISSUES_URL)
        assertEquals("$REPOSITORY_URL/releases", AppDistribution.releasesUrl)
        assertEquals("github.com", URI(AppDistribution.releasesUrl).host)
    }
    @Test fun commitLinksUseTheFork() {
        assertEquals("$REPOSITORY_URL/commit/abcdef", AppDistribution.sourceUrl("abcdef", null))
    }
    @Test fun taggedBuildsLinkToTheForkTag() {
        assertEquals("$REPOSITORY_URL/tree/v2.0.0", AppDistribution.sourceUrl("abcdef", "v2.0.0"))
    }
    @Test fun emptyTagsUseTheCommit() {
        assertEquals("$REPOSITORY_URL/commit/abcdef", AppDistribution.sourceUrl("abcdef", ""))
        assertEquals("$REPOSITORY_URL/commit/abcdef", AppDistribution.sourceUrl("abcdef", " "))
    }
    @Test fun upstreamAttributionIsRetainedSeparately() {
        assertEquals("https://github.com/gkd-kit/gkd", AppDistribution.upstreamRepositoryUrl)
        assertNotEquals(REPOSITORY_URL, AppDistribution.upstreamRepositoryUrl)
    }
    @Test fun upstreamApkUpdatesCannotReplaceTheFork() {
        assertFalse(AppDistribution.allowUpstreamApkUpdates)
    }
}
