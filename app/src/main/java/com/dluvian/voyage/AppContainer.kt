package com.dluvian.voyage

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.room.Room
import com.anggrayudi.storage.SimpleStorageHelper
import com.dluvian.voyage.core.ExternalSignerHandler
import com.dluvian.voyage.core.Topic
import com.dluvian.voyage.core.model.ConnectionStatus
import com.dluvian.voyage.data.account.AccountManager
import com.dluvian.voyage.data.account.AccountSwitcher
import com.dluvian.voyage.data.account.ExternalSigner
import com.dluvian.voyage.data.account.MnemonicSigner
import com.dluvian.voyage.data.event.EventCounter
import com.dluvian.voyage.data.event.EventDeletor
import com.dluvian.voyage.data.event.EventMaker
import com.dluvian.voyage.data.event.EventProcessor
import com.dluvian.voyage.data.event.EventQueue
import com.dluvian.voyage.data.event.EventRebroadcaster
import com.dluvian.voyage.data.event.EventSweeper
import com.dluvian.voyage.data.event.EventValidator
import com.dluvian.voyage.data.event.IdCacheClearer
import com.dluvian.voyage.data.event.OldestUsedEvent
import com.dluvian.voyage.data.inMemory.MetadataInMemory
import com.dluvian.voyage.data.interactor.Bookmarker
import com.dluvian.voyage.data.interactor.ItemSetEditor
import com.dluvian.voyage.data.interactor.Muter
import com.dluvian.voyage.data.interactor.PostSender
import com.dluvian.voyage.data.interactor.PostVoter
import com.dluvian.voyage.data.interactor.ProfileFollower
import com.dluvian.voyage.data.interactor.ThreadCollapser
import com.dluvian.voyage.data.interactor.TopicFollower
import com.dluvian.voyage.data.nostr.LazyNostrSubscriber
import com.dluvian.voyage.data.nostr.NostrClient
import com.dluvian.voyage.data.nostr.NostrService
import com.dluvian.voyage.data.nostr.NostrSubscriber
import com.dluvian.voyage.data.nostr.RelayUrl
import com.dluvian.voyage.data.nostr.SubBatcher
import com.dluvian.voyage.data.nostr.SubId
import com.dluvian.voyage.data.nostr.SubscriptionCreator
import com.dluvian.voyage.data.preferences.DatabasePreferences
import com.dluvian.voyage.data.preferences.RelayPreferences
import com.dluvian.voyage.data.provider.AnnotatedStringProvider
import com.dluvian.voyage.data.provider.DatabaseInteractor
import com.dluvian.voyage.data.provider.FeedProvider
import com.dluvian.voyage.data.provider.FriendProvider
import com.dluvian.voyage.data.provider.ItemSetProvider
import com.dluvian.voyage.data.provider.MuteProvider
import com.dluvian.voyage.data.provider.NameProvider
import com.dluvian.voyage.data.provider.ProfileProvider
import com.dluvian.voyage.data.provider.PubkeyProvider
import com.dluvian.voyage.data.provider.RelayProfileProvider
import com.dluvian.voyage.data.provider.RelayProvider
import com.dluvian.voyage.data.provider.SearchProvider
import com.dluvian.voyage.data.provider.SuggestionProvider
import com.dluvian.voyage.data.provider.ThreadProvider
import com.dluvian.voyage.data.provider.TopicProvider
import com.dluvian.voyage.data.provider.WebOfTrustProvider
import com.dluvian.voyage.data.room.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import rust.nostr.protocol.EventId
import rust.nostr.protocol.Filter
import java.util.Collections

class AppContainer(context: Context, storageHelper: SimpleStorageHelper) {
    val roomDb: AppDatabase = Room.databaseBuilder(
        context = context,
        klass = AppDatabase::class.java,
        name = "voyage_database",
    ).build()

    // Shared collections
    private val syncedFilterCache = Collections
        .synchronizedMap(mutableMapOf<SubId, List<Filter>>())
    private val syncedIdCache = Collections.synchronizedSet(mutableSetOf<EventId>())

    val snackbar = SnackbarHostState()
    private val nostrClient = NostrClient()
    val mnemonicSigner = MnemonicSigner(context = context)
    val externalSignerHandler = ExternalSignerHandler()
    private val externalSigner = ExternalSigner(handler = externalSignerHandler)

    private val idCacheClearer = IdCacheClearer(
        syncedIdCache = syncedIdCache,
    )

    val connectionStatuses = mutableStateOf(mapOf<RelayUrl, ConnectionStatus>())

    private val forcedFollowTopicStates = MutableStateFlow(emptyMap<Topic, Boolean>())
    private val forcedMuteTopicStates = MutableStateFlow(emptyMap<Topic, Boolean>())

    private val accountManager = AccountManager(
        mnemonicSigner = mnemonicSigner,
        externalSigner = externalSigner,
        accountDao = roomDb.accountDao(),
    )

    private val friendProvider = FriendProvider(
        friendDao = roomDb.friendDao(),
        myPubkeyProvider = accountManager,
    )

    val muteProvider = MuteProvider(muteDao = roomDb.muteDao())

    val metadataInMemory = MetadataInMemory()

    private val nameProvider = NameProvider(
        profileDao = roomDb.profileDao(),
        metadataInMemory = metadataInMemory,
    )

    private val annotatedStringProvider = AnnotatedStringProvider(
        nameProvider = nameProvider
    )

    private val pubkeyProvider = PubkeyProvider(friendProvider = friendProvider)

    val relayPreferences = RelayPreferences(context = context)

    val relayProvider = RelayProvider(
        nip65Dao = roomDb.nip65Dao(),
        eventRelayDao = roomDb.eventRelayDao(),
        nostrClient = nostrClient,
        connectionStatuses = connectionStatuses,
        pubkeyProvider = pubkeyProvider,
        relayPreferences = relayPreferences,
    )

    val itemSetProvider = ItemSetProvider(
        room = roomDb,
        myPubkeyProvider = accountManager,
        friendProvider = friendProvider,
        muteProvider = muteProvider,
        annotatedStringProvider = annotatedStringProvider,
        relayProvider = relayProvider
    )

    val topicProvider = TopicProvider(
        forcedFollowStates = forcedFollowTopicStates,
        forcedMuteStates = forcedMuteTopicStates,
        topicDao = roomDb.topicDao(),
        muteDao = roomDb.muteDao(),
        itemSetProvider = itemSetProvider,
    )


    private val eventCounter = EventCounter()

    val subCreator = SubscriptionCreator(
        nostrClient = nostrClient,
        syncedFilterCache = syncedFilterCache,
        eventCounter = eventCounter
    )

    private val webOfTrustProvider = WebOfTrustProvider(
        myPubkeyProvider = accountManager,
        friendProvider = friendProvider,
        webOfTrustDao = roomDb.webOfTrustDao()
    )

    val lazyNostrSubscriber = LazyNostrSubscriber(
        room = roomDb,
        relayProvider = relayProvider,
        subCreator = subCreator,
        webOfTrustProvider = webOfTrustProvider,
        friendProvider = friendProvider,
        topicProvider = topicProvider,
        myPubkeyProvider = accountManager,
        itemSetProvider = itemSetProvider,
        pubkeyProvider = pubkeyProvider
    )

    private val subBatcher = SubBatcher(subCreator = subCreator)

    val nostrSubscriber = NostrSubscriber(
        topicProvider = topicProvider,
        myPubkeyProvider = accountManager,
        subCreator = subCreator,
        relayProvider = relayProvider,
        webOfTrustProvider = webOfTrustProvider,
        subBatcher = subBatcher,
        room = roomDb,
    )

    val accountSwitcher = AccountSwitcher(
        accountManager = accountManager,
        accountDao = roomDb.accountDao(),
        deleteDao = roomDb.deleteDao(),
        idCacheClearer = idCacheClearer,
        lazyNostrSubscriber = lazyNostrSubscriber,
        nostrSubscriber = nostrSubscriber
    )

    private val eventValidator = EventValidator(
        syncedFilterCache = syncedFilterCache,
        syncedIdCache = syncedIdCache,
        myPubkeyProvider = accountManager
    )
    private val eventProcessor = EventProcessor(
        room = roomDb,
        metadataInMemory = metadataInMemory,
        myPubkeyProvider = accountManager
    )
    private val eventQueue = EventQueue(
        eventValidator = eventValidator,
        eventProcessor = eventProcessor
    )
    private val eventMaker = EventMaker(accountManager = accountManager)

    val databasePreferences = DatabasePreferences(context = context)
    val databaseInteractor = DatabaseInteractor(
        room = roomDb,
        context = context,
        storageHelper = storageHelper,
        snackbar = snackbar
    )

    val nostrService = NostrService(
        nostrClient = nostrClient,
        eventQueue = eventQueue,
        eventMaker = eventMaker,
        filterCache = syncedFilterCache,
        relayPreferences = relayPreferences,
        connectionStatuses = connectionStatuses,
        eventCounter = eventCounter,
    )

    init {
        nameProvider.nostrSubscriber = nostrSubscriber
        pubkeyProvider.itemSetProvider = itemSetProvider
        nostrService.initialize(initRelayUrls = relayProvider.getReadRelays())
    }

    val eventDeletor = EventDeletor(
        snackbar = snackbar,
        nostrService = nostrService,
        context = context,
        relayProvider = relayProvider,
        deleteDao = roomDb.deleteDao()
    )

    val postVoter = PostVoter(
        nostrService = nostrService,
        relayProvider = relayProvider,
        snackbar = snackbar,
        context = context,
        voteDao = roomDb.voteDao(),
        eventDeletor = eventDeletor,
    )

    val threadCollapser = ThreadCollapser()

    val topicFollower = TopicFollower(
        nostrService = nostrService,
        relayProvider = relayProvider,
        topicUpsertDao = roomDb.topicUpsertDao(),
        topicDao = roomDb.topicDao(),
        snackbar = snackbar,
        context = context,
        forcedFollowStates = forcedFollowTopicStates
    )

    val bookmarker = Bookmarker(
        nostrService = nostrService,
        relayProvider = relayProvider,
        bookmarkUpsertDao = roomDb.bookmarkUpsertDao(),
        bookmarkDao = roomDb.bookmarkDao(),
        snackbar = snackbar,
        context = context,
    )

    val muter = Muter(
        forcedTopicMuteFlow = forcedMuteTopicStates,
        nostrService = nostrService,
        relayProvider = relayProvider,
        muteUpsertDao = roomDb.muteUpsertDao(),
        muteDao = roomDb.muteDao(),
        snackbar = snackbar,
        context = context,
    )

    private val oldestUsedEvent = OldestUsedEvent()

    val profileFollower = ProfileFollower(
        nostrService = nostrService,
        relayProvider = relayProvider,
        snackbar = snackbar,
        context = context,
        friendProvider = friendProvider,
        friendUpsertDao = roomDb.friendUpsertDao(),
    )

    val feedProvider = FeedProvider(
        nostrSubscriber = nostrSubscriber,
        room = roomDb,
        oldestUsedEvent = oldestUsedEvent,
        annotatedStringProvider = annotatedStringProvider,
        forcedVotes = postVoter.forcedVotes,
        forcedFollows = profileFollower.forcedFollowsFlow,
        forcedBookmarks = bookmarker.forcedBookmarksFlow
    )

    val threadProvider = ThreadProvider(
        nostrSubscriber = nostrSubscriber,
        lazyNostrSubscriber = lazyNostrSubscriber,
        room = roomDb,
        collapsedIds = threadCollapser.collapsedIds,
        annotatedStringProvider = annotatedStringProvider,
        oldestUsedEvent = oldestUsedEvent,
        forcedVotes = postVoter.forcedVotes,
        forcedFollows = profileFollower.forcedFollowsFlow,
        forcedBookmarks = bookmarker.forcedBookmarksFlow,
    )

    val profileProvider = ProfileProvider(
        forcedFollowFlow = profileFollower.forcedFollowsFlow,
        forcedMuteFlow = muter.forcedProfileMuteFlow,
        myPubkeyProvider = accountManager,
        metadataInMemory = metadataInMemory,
        room = roomDb,
        friendProvider = friendProvider,
        muteProvider = muteProvider,
        itemSetProvider = itemSetProvider,
        lazyNostrSubscriber = lazyNostrSubscriber,
        nostrSubscriber = nostrSubscriber,
        annotatedStringProvider = annotatedStringProvider,
    )

    val searchProvider = SearchProvider(
        topicProvider = topicProvider,
        profileProvider = profileProvider,
        postDao = roomDb.postDao()
    )

    val suggestionProvider = SuggestionProvider(
        searchProvider = searchProvider
    )

    val postSender = PostSender(
        nostrService = nostrService,
        relayProvider = relayProvider,
        postInsertDao = roomDb.postInsertDao(),
        postDao = roomDb.postDao(),
        myPubkeyProvider = accountManager
    )

    val eventSweeper = EventSweeper(
        databasePreferences = databasePreferences,
        idCacheClearer = idCacheClearer,
        deleteDao = roomDb.deleteDao(),
        oldestUsedEvent = oldestUsedEvent
    )

    val relayProfileProvider = RelayProfileProvider()

    val eventRebroadcaster = EventRebroadcaster(
        nostrService = nostrService,
        postDao = roomDb.postDao(),
        relayProvider = relayProvider,
        snackbar = snackbar,
    )

    val itemSetEditor = ItemSetEditor(
        nostrService = nostrService,
        relayProvider = relayProvider,
        profileSetUpsertDao = roomDb.profileSetUpsertDao(),
        topicSetUpsertDao = roomDb.topicSetUpsertDao(),
        itemSetProvider = itemSetProvider,
    )
}
