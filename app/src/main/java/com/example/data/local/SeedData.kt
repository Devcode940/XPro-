package com.example.data.local

import com.example.data.model.BookmarkFolder
import com.example.data.model.BookmarkItemType
import com.example.data.model.BrowserHistoryItem
import com.example.data.model.CommunityItem
import com.example.data.model.DirectMessage
import com.example.data.model.LibraryMediaKind
import com.example.data.model.MediaLibraryItem
import com.example.data.model.MessageConversation
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationKind
import com.example.data.model.PlaylistItem
import com.example.data.model.PollData
import com.example.data.model.PollOption
import com.example.data.model.PostItem
import com.example.data.model.PostRevision
import com.example.data.model.SavedPageItem
import com.example.data.model.SuggestedUser
import com.example.data.model.TrendItem
import com.example.data.model.TypedBookmarkItem
import com.example.data.model.UserProfile
import com.example.data.model.VerificationBadge
import com.example.data.model.XListItem

object SeedData {

    private val now = System.currentTimeMillis()
    private fun minsAgo(mins: Long) = now - mins * 60_000L
    private fun hoursAgo(hours: Long) = now - hours * 3_600_000L

    val initialProfile = UserProfile(
        id = "current_user",
        name = "Elena Vance",
        handle = "@elenavance",
        bio = "Staff Systems Architect · Building local-first software, OLED interfaces, and low-latency tooling. X Pro Unlocked active.",
        badge = VerificationBadge.FOUNDER,
        joinedDate = "Joined March 2019",
        followingCount = 342,
        followersCount = 18_420,
        undoDelaySeconds = 5,
        adFreeEnabled = true,
        extendedPostsEnabled = true,
        showEditHistoryLinks = true,
        autoExpandThreads = false
    )

    val initialFolders = listOf(
        BookmarkFolder(
            id = "folder_eng",
            name = "Engineering",
            description = "Distributed systems, compilers, and runtime notes",
            colorHex = 0xFF1D9BF0,
            iconKey = "code"
        ),
        BookmarkFolder(
            id = "folder_design",
            name = "Design Systems",
            description = "OLED typography, grids, and motion tokens",
            colorHex = 0xFFE2B714,
            iconKey = "palette"
        ),
        BookmarkFolder(
            id = "folder_alpha",
            name = "Market Alpha",
            description = "Semiconductors, compute economics, and filings",
            colorHex = 0xFF00BA7C,
            iconKey = "trending"
        ),
        BookmarkFolder(
            id = "folder_reading",
            name = "Reading List",
            description = "Long-form essays and deep technical threads",
            colorHex = 0xFFA855F7,
            iconKey = "book"
        )
    )

    val initialTypedBookmarks = listOf(
        TypedBookmarkItem(
            id = "tb_post_article_1",
            itemType = BookmarkItemType.POST,
            title = "Why Sub-10ms Local-First State Beats Cloud Roundtrips",
            content = "Every time a social client waits 350ms on a remote API just to toggle a bookmark or save a post revision, users feel subtle micro-friction.",
            authorName = "Kairo Systems",
            authorHandle = "@kairosystems",
            folderId = "folder_eng",
            postId = "post_article_1",
            createdAtMillis = minsAgo(36)
        ),
        TypedBookmarkItem(
            id = "tb_post_thread_1",
            itemType = BookmarkItemType.POST,
            title = "OLED Pure Black #000000 Visual Breakdown",
            content = "1/4 Why pure #000000 OLED black combined with #2F3336 hairline borders still outperforms frosted gray cards for high-density reading feeds.",
            authorName = "Mira Sato",
            authorHandle = "@mirasato_ui",
            folderId = "folder_design",
            postId = "post_thread_1",
            createdAtMillis = hoursAgo(1)
        ),
        TypedBookmarkItem(
            id = "tb_video_1",
            itemType = BookmarkItemType.VIDEO,
            title = "For Bigger Blazes — 1080p Reference Stream",
            content = "Saved HD MP4 demo stream for hardware AV1/H.264 decoder testing & offline playback.",
            authorName = "Devon Mercer",
            authorHandle = "@devonmercer",
            mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            folderId = "folder_eng",
            createdAtMillis = hoursAgo(2)
        ),
        TypedBookmarkItem(
            id = "tb_image_1",
            itemType = BookmarkItemType.IMAGE,
            title = "OLED Grid Typography Reference Sheet",
            content = "Contrast luminance tokens & hairline border ratios for dark/light adaptive feeds.",
            authorName = "Mira Sato",
            authorHandle = "@mirasato_ui",
            mediaUrl = "drawable://media_post_1",
            folderId = "folder_design",
            createdAtMillis = hoursAgo(3)
        ),
        TypedBookmarkItem(
            id = "tb_link_1",
            itemType = BookmarkItemType.LINK,
            title = "Portable Bookmark Archive Specification v2.4",
            content = "Open schema draft for portable user bookmark folders and cross-device sync.",
            authorName = "OpenSpec Foundation",
            authorHandle = "@openspec_org",
            linkUrl = "https://developer.android.com/jetpack/compose",
            folderId = "folder_reading",
            postId = "post_protocol_1",
            createdAtMillis = hoursAgo(4)
        )
    )

    val initialSuggestedUsers = listOf(
        SuggestedUser(
            id = "author_kairo",
            name = "Kairo Systems",
            handle = "@kairosystems",
            bio = "Zero-copy storage engines & distributed consensus.",
            badge = VerificationBadge.GOLD,
            isFollowing = true,
            avatarColorHex = 0xFF1D9BF0
        ),
        SuggestedUser(
            id = "author_mira",
            name = "Mira Sato",
            handle = "@mirasato_ui",
            bio = "Design engineer crafting high-contrast mobile & desktop systems.",
            badge = VerificationBadge.BLUE,
            isFollowing = true,
            avatarColorHex = 0xFFE2B714
        ),
        SuggestedUser(
            id = "author_protocol",
            name = "OpenSpec Foundation",
            handle = "@openspec_org",
            bio = "Official standards working group for open client protocols.",
            badge = VerificationBadge.GRAY,
            isFollowing = false,
            avatarColorHex = 0xFF829AAB
        ),
        SuggestedUser(
            id = "author_devon",
            name = "Devon Mercer",
            handle = "@devonmercer",
            bio = "Silicon supply chain analyst & hardware teardowns.",
            badge = VerificationBadge.BLUE,
            isFollowing = true,
            avatarColorHex = 0xFF00BA7C
        )
    )

    val initialTrends = listOf(
        TrendItem(
            id = "trend_1",
            category = "Technology · Trending",
            topic = "#LocalFirst",
            postCountLabel = "24.8K posts",
            isPromoted = false
        ),
        TrendItem(
            id = "trend_ad_1",
            category = "Promoted · Cloud Infrastructure",
            topic = "HyperScale Cloud Credits — Claim $5,000 Today",
            postCountLabel = "Sponsored by HyperScale",
            isPromoted = true
        ),
        TrendItem(
            id = "trend_2",
            category = "Engineering · Trending",
            topic = "Jetpack Compose 2.0",
            postCountLabel = "18.2K posts",
            isPromoted = false
        ),
        TrendItem(
            id = "trend_3",
            category = "Design · Trending",
            topic = "OLED Pure Black",
            postCountLabel = "9,410 posts",
            isPromoted = false
        ),
        TrendItem(
            id = "trend_ad_2",
            category = "Promoted · Fintech",
            topic = "ApexCard Business — 4% Unlimited Cashback",
            postCountLabel = "Sponsored by ApexCard",
            isPromoted = true
        ),
        TrendItem(
            id = "trend_4",
            category = "Hardware · Trending",
            topic = "3nm Mobile NPU",
            postCountLabel = "41.5K posts",
            isPromoted = false
        ),
        TrendItem(
            id = "trend_5",
            category = "Culture · Trending",
            topic = "Ad-Free Web",
            postCountLabel = "63.1K posts",
            isPromoted = false
        )
    )

    val initialPosts = listOf(
        PostItem(
            id = "post_user_1",
            authorId = "current_user",
            authorName = initialProfile.name,
            authorHandle = initialProfile.handle,
            authorBio = initialProfile.bio,
            authorBadge = initialProfile.badge,
            authorAvatarColorHex = 0xFF1D9BF0,
            isCurrentUser = true,
            isFollowedAuthor = true,
            content = "Just unlocked full local-first editing, custom verification tiers, and zero-ad timelines in #XPro. Having an explicit revision log with diffs plus a 5-second undo timer completely removes posting anxiety.",
            createdAtMillis = minsAgo(14),
            editedAtMillis = minsAgo(9),
            likes = 412,
            reposts = 68,
            replies = 29,
            views = 14_820,
            isHighlight = true,
            isSavedForOffline = true,
            offlineCachedAtMillis = minsAgo(12),
            revisions = listOf(
                PostRevision(
                    content = "Testing local-first editing and 5-second undo timer in #XPro. Zero ads makes the timeline feel twice as fast.",
                    editedAtMillis = minsAgo(14),
                    reason = "Initial version"
                )
            )
        ),
        PostItem(
            id = "post_ad_1",
            authorId = "ad_nebula",
            authorName = "Nebula Compute Cloud",
            authorHandle = "@nebulacloud",
            authorBio = "Enterprise GPU clusters on demand.",
            authorBadge = VerificationBadge.GOLD,
            authorAvatarColorHex = 0xFF6366F1,
            isFollowedAuthor = false,
            content = "Scale your inference clusters 10x faster with Nebula Bare-Metal GPUs. Start your 14-day enterprise trial and lock in reserved H200 pricing before Q4 ends. #Sponsored",
            createdAtMillis = minsAgo(18),
            likes = 34,
            reposts = 4,
            replies = 2,
            views = 98_200,
            isSponsored = true,
            sponsorLabel = "Promoted · Nebula Compute"
        ),
        PostItem(
            id = "post_article_1",
            authorId = "author_kairo",
            authorName = "Kairo Systems",
            authorHandle = "@kairosystems",
            authorBio = "Zero-copy storage engines & distributed consensus.",
            authorBadge = VerificationBadge.GOLD,
            authorAvatarColorHex = 0xFF1D9BF0,
            isFollowedAuthor = true,
            isLongForm = true,
            articleTitle = "Why Sub-10ms Local-First State Beats Cloud Roundtrips",
            articleSubtitle = "Designing deterministic optimistic mutations, immutable revision logs, and zero-flicker timelines.",
            content = """# The Latency Tax on Modern Feeds

Every time a social client waits 350ms on a remote API just to toggle a bookmark or save a post revision, users feel subtle micro-friction. Over a 20-minute reading session, that lag compounds into cognitive fatigue.

## Three Pillars of Instant Clients

- **Immediate SQLite / Room Writes:** Persist mutations locally in under 4ms before any background sync runs.
- **Immutable Revision History:** Never overwrite user prose silently; store every edit snapshot with a clear diff trail.
- **Configurable Undo Windows:** A 5-second local dispatch queue catches typos before they ever touch the wire.

> "Speed is not just a benchmark metric — it is the primary emotional texture of software craftsmanship."

```kotlin
suspend fun commitWithUndo(draft: DraftPost, delayMs: Long) {
    delay(delayMs)
    localDatabase.insertPost(draft.toEntity())
}
```

When you strip out promoted ad injectors and tracking pixels from the feed pipeline, frame pacing stabilizes at a locked 120Hz on OLED panels.""",
            createdAtMillis = minsAgo(36),
            likes = 2_840,
            reposts = 612,
            replies = 148,
            views = 184_500,
            isBookmarked = true,
            bookmarkFolderId = "folder_eng",
            mediaResKey = "article_cover_1",
            isSavedForOffline = true,
            offlineCachedAtMillis = minsAgo(30)
        ),
        PostItem(
            id = "post_thread_1",
            authorId = "author_mira",
            authorName = "Mira Sato",
            authorHandle = "@mirasato_ui",
            authorBio = "Design engineer crafting high-contrast mobile & desktop systems.",
            authorBadge = VerificationBadge.BLUE,
            authorAvatarColorHex = 0xFFE2B714,
            isFollowedAuthor = true,
            content = "1/4 Why pure #000000 OLED black combined with #2F3336 hairline borders still outperforms frosted gray cards for high-density reading feeds. A quick visual systems breakdown 🧵",
            createdAtMillis = hoursAgo(1),
            likes = 1_520,
            reposts = 294,
            replies = 67,
            views = 92_300,
            isBookmarked = true,
            bookmarkFolderId = "folder_design",
            mediaResKey = "media_post_1",
            isSavedForOffline = true,
            offlineCachedAtMillis = hoursAgo(1),
            threadPosts = listOf(
                "1/4 Why pure #000000 OLED black combined with #2F3336 hairline borders still outperforms frosted gray cards for high-density reading feeds. A quick visual systems breakdown 🧵",
                "2/4 Elevated gray cards force your eye to track rectangular containers on every scroll tick. On an OLED screen, turning pixels completely off removes container noise and lets typography breathe.",
                "3/4 Pair Plus Jakarta Sans for UI headings with DM Sans for long-form prose and tabular JetBrains Mono for engagement counts. Tabular numbers prevent horizontal layout jitter when live counters tick up.",
                "4/4 Finally, drop metadata pills for timestamps and view counts. Quiet middle-dot separators (14m · 14.8K views) keep the reading rhythm calm and scannable."
            )
        ),
        PostItem(
            id = "post_poll_1",
            authorId = "author_devon",
            authorName = "Devon Mercer",
            authorHandle = "@devonmercer",
            authorBio = "Silicon supply chain analyst & hardware teardowns.",
            authorBadge = VerificationBadge.BLUE,
            authorAvatarColorHex = 0xFF00BA7C,
            isFollowedAuthor = true,
            content = "Which unlocked client capability changes your daily workflow the most? Curious where power users spend 80% of their time.",
            createdAtMillis = hoursAgo(2),
            likes = 784,
            reposts = 92,
            replies = 114,
            views = 47_600,
            poll = PollData(
                question = "Most valuable unlocked power feature",
                options = listOf(
                    PollOption("opt_1", "Zero-Ad Timeline & Trends", 1_420),
                    PollOption("opt_2", "Bookmark Folders & Search", 685),
                    PollOption("opt_3", "Post Editing + Undo Send", 512),
                    PollOption("opt_4", "Long Articles & Reader Mode", 398)
                ),
                selectedOptionId = null,
                durationLabel = "18h remaining"
            )
        ),
        PostItem(
            id = "post_ad_2",
            authorId = "ad_quant",
            authorName = "QuantDesk Pro",
            authorHandle = "@quantdesk_ai",
            authorBio = "Automated portfolio analytics.",
            authorBadge = VerificationBadge.BLUE,
            authorAvatarColorHex = 0xFFF59E0B,
            isFollowedAuthor = false,
            content = "Stop guessing market rotations. QuantDesk Pro delivers real-time institutional order flow alerts directly to your phone. Tap to unlock 30 days free! #Ad",
            createdAtMillis = hoursAgo(3),
            likes = 19,
            reposts = 2,
            replies = 1,
            views = 120_400,
            isSponsored = true,
            sponsorLabel = "Promoted · QuantDesk"
        ),
        PostItem(
            id = "post_protocol_1",
            authorId = "author_protocol",
            authorName = "OpenSpec Foundation",
            authorHandle = "@openspec_org",
            authorBio = "Official standards working group for open client protocols.",
            authorBadge = VerificationBadge.GRAY,
            authorAvatarColorHex = 0xFF829AAB,
            isFollowedAuthor = false,
            content = "Draft v2.4 of the Portable Bookmark Archive spec is now open for public review. Standardized folder schemas mean your curated research library never gets locked inside a proprietary feed.",
            createdAtMillis = hoursAgo(4),
            likes = 930,
            reposts = 215,
            replies = 43,
            views = 53_100,
            isBookmarked = true,
            bookmarkFolderId = "folder_reading",
            isSavedForOffline = true,
            offlineCachedAtMillis = hoursAgo(3)
        ),
        PostItem(
            id = "post_article_2",
            authorId = "author_mira",
            authorName = "Mira Sato",
            authorHandle = "@mirasato_ui",
            authorBio = "Design engineer crafting high-contrast mobile & desktop systems.",
            authorBadge = VerificationBadge.BLUE,
            authorAvatarColorHex = 0xFFE2B714,
            isFollowedAuthor = true,
            isLongForm = true,
            articleTitle = "Typography Hierarchy for High-Density Social Readers",
            articleSubtitle = "Balancing scan speed, serif comfort in Reader Mode, and tabular metrics.",
            content = """# Designing Calm High-Density Streams

Most social feeds feel exhausting because every element shouts at the exact same contrast level. Effective dark mode design relies on deliberate luminance steps.

## Luminance Tiers on OLED

- **Primary Prose (#E7E9EA):** 91% off-white avoids harsh halation against pure `#000000` black.
- **Secondary Metadata (#71767B):** Keeps handles, timestamps, and view metrics legible without competing with the post body.
- **Hairline Structure (#2F3336):** 1dp dividers define rhythm without heavy card chrome.

> "Good editorial software disappears the moment you start reading a great essay."

Try switching between **Sans**, **Serif**, and **Mono** inside Reader Mode to see how font cadence shapes comprehension.""",
            createdAtMillis = hoursAgo(6),
            likes = 1_190,
            reposts = 240,
            replies = 52,
            views = 68_900,
            isBookmarked = true,
            bookmarkFolderId = "folder_design",
            isSavedForOffline = true,
            offlineCachedAtMillis = hoursAgo(4)
        ),
        PostItem(
            id = "post_market_1",
            authorId = "author_devon",
            authorName = "Devon Mercer",
            authorHandle = "@devonmercer",
            authorBio = "Silicon supply chain analyst & hardware teardowns.",
            authorBadge = VerificationBadge.BLUE,
            authorAvatarColorHex = 0xFF00BA7C,
            isFollowedAuthor = true,
            content = "Wafer yield numbers on 3nm mobile SoCs crossed 82% this quarter, unlocking sustained on-device NPU throughput under 4W. Expect local multimodal indexing to become standard across flagship phones in 2026. ${'$'}TSM #Semiconductors",
            createdAtMillis = hoursAgo(8),
            likes = 2_110,
            reposts = 487,
            replies = 89,
            views = 142_000,
            isBookmarked = true,
            bookmarkFolderId = "folder_alpha"
        ),
        PostItem(
            id = "post_ad_3",
            authorId = "ad_velox",
            authorName = "Velox VPN Ultra",
            authorHandle = "@veloxvpn",
            authorBio = "Fast private tunneling.",
            authorBadge = VerificationBadge.BLUE,
            authorAvatarColorHex = 0xFF38BDF8,
            isFollowedAuthor = false,
            content = "Protect every packet on public Wi-Fi with Velox VPN Ultra. 80% off our 2-year creator bundle today only! #Promoted",
            createdAtMillis = hoursAgo(9),
            likes = 11,
            reposts = 1,
            replies = 0,
            views = 76_400,
            isSponsored = true,
            sponsorLabel = "Promoted · Velox VPN"
        ),
        PostItem(
            id = "post_user_2",
            authorId = "current_user",
            authorName = initialProfile.name,
            authorHandle = initialProfile.handle,
            authorBio = initialProfile.bio,
            authorBadge = initialProfile.badge,
            authorAvatarColorHex = 0xFF1D9BF0,
            isCurrentUser = true,
            isFollowedAuthor = true,
            content = "Pro tip: organize your saved threads into color-coded Bookmark Folders (Engineering, Design Systems, Market Alpha) so your best finds don't turn into a chronological graveyard.",
            createdAtMillis = hoursAgo(12),
            likes = 538,
            reposts = 104,
            replies = 18,
            views = 19_450,
            isHighlight = true
        ),
        PostItem(
            id = "post_kairo_2",
            authorId = "author_kairo",
            authorName = "Kairo Systems",
            authorHandle = "@kairosystems",
            authorBio = "Zero-copy storage engines & distributed consensus.",
            authorBadge = VerificationBadge.GOLD,
            authorAvatarColorHex = 0xFF1D9BF0,
            isFollowedAuthor = true,
            content = "Paging 3 backed directly by Room's SQLite PagingSource eliminates manual offset bookkeeping. Even with 10,000+ cached timeline entries, scroll memory stays flat.",
            createdAtMillis = hoursAgo(15),
            likes = 642,
            reposts = 131,
            replies = 27,
            views = 38_900
        ),
        PostItem(
            id = "post_mira_2",
            authorId = "author_mira",
            authorName = "Mira Sato",
            authorHandle = "@mirasato_ui",
            authorBio = "Design engineer crafting high-contrast mobile & desktop systems.",
            authorBadge = VerificationBadge.BLUE,
            authorAvatarColorHex = 0xFFE2B714,
            isFollowedAuthor = true,
            content = "Tip for Compose developers: keep touch targets at 48dp minimum on all icon actions, and always pair icon indicators with semantic content descriptions.",
            createdAtMillis = hoursAgo(18),
            likes = 890,
            reposts = 175,
            replies = 34,
            views = 51_200
        ),
        PostItem(
            id = "post_devon_2",
            authorId = "author_devon",
            authorName = "Devon Mercer",
            authorHandle = "@devonmercer",
            authorBio = "Silicon supply chain analyst & hardware teardowns.",
            authorBadge = VerificationBadge.BLUE,
            authorAvatarColorHex = 0xFF00BA7C,
            isFollowedAuthor = true,
            content = "Hardware media decoders on Android now handle 10-bit AV1 & WebM streams at 60fps with negligible thermal throttling. Direct MP4/WebM downloads via DownloadManager pair great with local players.",
            createdAtMillis = hoursAgo(22),
            likes = 514,
            reposts = 88,
            replies = 21,
            views = 29_700
        )
    )

    val initialNotifications = listOf(
        NotificationItem(
            id = "notif_1",
            kind = NotificationKind.LIKE,
            actorName = "Kairo Systems",
            actorHandle = "@kairosystems",
            actorAvatarHex = 0xFF1D9BF0,
            message = "liked your post about local-first editing and 5s undo send.",
            targetPostId = "post_user_1",
            createdAtMillis = minsAgo(8),
            isRead = false
        ),
        NotificationItem(
            id = "notif_2",
            kind = NotificationKind.COMMENT,
            actorName = "Mira Sato",
            actorHandle = "@mirasato_ui",
            actorAvatarHex = 0xFFE2B714,
            message = "replied: \"Hairline #2F3336 borders with pure #000000 OLED make scanning effortless.\"",
            targetPostId = "post_user_2",
            createdAtMillis = minsAgo(24),
            isRead = false
        ),
        NotificationItem(
            id = "notif_3",
            kind = NotificationKind.FOLLOWER,
            actorName = "OpenSpec Foundation",
            actorHandle = "@openspec_org",
            actorAvatarHex = 0xFF829AAB,
            message = "followed you back and subscribed to your Engineering list.",
            createdAtMillis = hoursAgo(1),
            isRead = true
        ),
        NotificationItem(
            id = "notif_4",
            kind = NotificationKind.MESSAGE,
            actorName = "Devon Mercer",
            actorHandle = "@devonmercer",
            actorAvatarHex = 0xFF00BA7C,
            message = "sent you a direct message about 3nm NPU benchmarks.",
            createdAtMillis = hoursAgo(2),
            isRead = false
        ),
        NotificationItem(
            id = "notif_5",
            kind = NotificationKind.DOWNLOAD,
            actorName = "XTRA Download Manager",
            actorHandle = "@system",
            actorAvatarHex = 0xFFA855F7,
            message = "Offline thread & article cache ready for airplane reading.",
            createdAtMillis = hoursAgo(4),
            isRead = true
        )
    )

    val initialConversations = listOf(
        MessageConversation(
            id = "conv_kairo",
            participantName = "Kairo Systems",
            participantHandle = "@kairosystems",
            participantBadge = VerificationBadge.GOLD,
            avatarColorHex = 0xFF1D9BF0,
            lastMessage = "Did you benchmark the Room PagingSource batch size at 20?",
            updatedAtMillis = minsAgo(15),
            unreadCount = 2,
            isEncrypted = true
        ),
        MessageConversation(
            id = "conv_mira",
            participantName = "Mira Sato",
            participantHandle = "@mirasato_ui",
            participantBadge = VerificationBadge.BLUE,
            avatarColorHex = 0xFFE2B714,
            lastMessage = "Sent the updated dark/light palette tokens for XTRA!",
            updatedAtMillis = hoursAgo(1),
            unreadCount = 0,
            isEncrypted = true
        ),
        MessageConversation(
            id = "conv_devon",
            participantName = "Devon Mercer",
            participantHandle = "@devonmercer",
            participantBadge = VerificationBadge.BLUE,
            avatarColorHex = 0xFF00BA7C,
            lastMessage = "Hardware AV1 test stream link is pinned in Media Center.",
            updatedAtMillis = hoursAgo(3),
            unreadCount = 1,
            isEncrypted = true
        )
    )

    val initialDirectMessages = listOf(
        DirectMessage("dm_1", "conv_kairo", false, "Hey Elena — saw your thread on sub-10ms local mutations.", hoursAgo(2)),
        DirectMessage("dm_2", "conv_kairo", true, "Thanks! Putting all timeline reads straight through SQLite PagingSource eliminated jitter.", hoursAgo(1)),
        DirectMessage("dm_3", "conv_kairo", false, "Did you benchmark the Room PagingSource batch size at 20?", minsAgo(15)),
        DirectMessage("dm_4", "conv_mira", false, "Sent the updated dark/light palette tokens for XTRA!", hoursAgo(1)),
        DirectMessage("dm_5", "conv_devon", false, "Hardware AV1 test stream link is pinned in Media Center.", hoursAgo(3))
    )

    val initialXLists = listOf(
        XListItem(
            id = "list_systems",
            name = "Systems & Compilers",
            description = "Kernel engineers, database architects, and local-first pioneers.",
            memberCount = 48,
            subscriberCount = 12_400,
            isJoined = true,
            isPrivate = false
        ),
        XListItem(
            id = "list_compose",
            name = "Android Jetpack Craft",
            description = "Compose rendering internals, Paging 3, Material 3, and CameraX.",
            memberCount = 34,
            subscriberCount = 8_920,
            isJoined = true,
            isPrivate = false
        ),
        XListItem(
            id = "list_silicon",
            name = "Semiconductor & NPU Watch",
            description = "Foundry yields, mobile NPUs, and edge inference papers.",
            memberCount = 29,
            subscriberCount = 5_110,
            isJoined = false,
            isPrivate = false
        )
    )

    val initialCommunities = listOf(
        CommunityItem(
            id = "comm_localfirst",
            name = "Local-First Software",
            category = "Engineering",
            description = "CRDTs, SQLite/Room architectures, offline-first sync, and instant UIs.",
            memberCount = 38_400,
            isJoined = true,
            accentHex = 0xFF1D9BF0
        ),
        CommunityItem(
            id = "comm_oled_design",
            name = "Minimalist OLED Design",
            category = "Design",
            description = "Typography, high-contrast palettes, motion systems, and accessibility.",
            memberCount = 21_950,
            isJoined = true,
            accentHex = 0xFFE2B714
        ),
        CommunityItem(
            id = "comm_open_protocols",
            name = "Open Social Protocols",
            category = "Standards",
            description = "Portable identity, interoperable feeds, and user-owned archives.",
            memberCount = 14_200,
            isJoined = false,
            accentHex = 0xFF00BA7C
        )
    )

    val initialPlaylists = listOf(
        PlaylistItem("pl_focus", "Late Night Architecture", "Ambient & instrumental tracks for deep focus", hoursAgo(48)),
        PlaylistItem("pl_demos", "AV1 & WebM Test Reels", "Hardware decoder benchmark clips & talks", hoursAgo(24))
    )

    val initialMediaLibrary = listOf(
        MediaLibraryItem(
            id = "media_vid_1",
            title = "For Bigger Blazes (1080p MP4)",
            creator = "Google Sample Streams",
            mediaKind = LibraryMediaKind.VIDEO,
            sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            durationLabel = "0:15",
            resolution = "1080p HD",
            sizeLabel = "2.4 MB",
            isFavorite = true,
            lastPlayedMillis = minsAgo(40),
            playCount = 4,
            playlistId = "pl_demos"
        ),
        MediaLibraryItem(
            id = "media_vid_2",
            title = "Big Buck Bunny Trailer (WebM)",
            creator = "Blender Open Movie",
            mediaKind = LibraryMediaKind.VIDEO,
            sourceUrl = "https://dl8.webmfiles.org/big-buck-bunny_trailer.webm",
            durationLabel = "0:32",
            resolution = "720p WebM",
            sizeLabel = "4.1 MB",
            isFavorite = false,
            lastPlayedMillis = hoursAgo(3),
            playCount = 2,
            playlistId = "pl_demos"
        ),
        MediaLibraryItem(
            id = "media_aud_1",
            title = "SoundHelix Synth Session #1",
            creator = "SoundHelix Studio",
            mediaKind = LibraryMediaKind.AUDIO,
            sourceUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            durationLabel = "6:12",
            resolution = "320 kbps MP3",
            sizeLabel = "8.5 MB",
            isFavorite = true,
            lastPlayedMillis = minsAgo(18),
            playCount = 7,
            playlistId = "pl_focus"
        ),
        MediaLibraryItem(
            id = "media_aud_2",
            title = "SoundHelix Ambient Session #2",
            creator = "SoundHelix Studio",
            mediaKind = LibraryMediaKind.AUDIO,
            sourceUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            durationLabel = "7:05",
            resolution = "320 kbps MP3",
            sizeLabel = "9.8 MB",
            isFavorite = false,
            lastPlayedMillis = null,
            playCount = 0,
            playlistId = "pl_focus"
        )
    )

    val initialSavedPages = listOf(
        SavedPageItem(
            id = "page_1",
            title = "Jetpack Compose Architecture Guide",
            url = "https://developer.android.com/develop/ui/compose/architecture",
            summary = "Unidirectional data flow, state hoisting, and ViewModel lifecycle recommendations.",
            savedAtMillis = hoursAgo(2),
            isInReadingList = true,
            isCompleted = false
        ),
        SavedPageItem(
            id = "page_2",
            title = "Material Design 3 Adaptive Navigation",
            url = "https://m3.material.io/components/navigation-drawer/overview",
            summary = "Modal, dismissible, and permanent navigation drawer patterns across window size classes.",
            savedAtMillis = hoursAgo(6),
            isInReadingList = true,
            isCompleted = true
        ),
        SavedPageItem(
            id = "page_3",
            title = "Android DownloadManager Reference",
            url = "https://developer.android.com/reference/android/app/DownloadManager",
            summary = "Long-running HTTP downloads with system notifications, Wi-Fi constraints, and retries.",
            savedAtMillis = hoursAgo(12),
            isInReadingList = false,
            isCompleted = false
        )
    )

    val initialBrowserHistory = listOf(
        BrowserHistoryItem(
            id = "hist_1",
            title = "Jetpack Compose Architecture Guide",
            url = "https://developer.android.com/develop/ui/compose/architecture",
            visitedAtMillis = minsAgo(55)
        ),
        BrowserHistoryItem(
            id = "hist_2",
            title = "OpenSpec Portable Archive Draft",
            url = "https://developer.android.com/topic/libraries/architecture/paging/v3-overview",
            visitedAtMillis = hoursAgo(3)
        )
    )
}
